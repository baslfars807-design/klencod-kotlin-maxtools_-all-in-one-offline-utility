package com.example.engine

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

data class TransferState(
    val isRunning: Boolean = false,
    val isServer: Boolean = false,
    val currentFileName: String = "",
    val filesSentOrReceived: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMbps: Float = 0f,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val error: String? = null,
    val isCompleted: Boolean = false
)

object P2PSharingEngine {

    const val DEFAULT_PORT = 8989
    val isCancelled = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null

    fun cancel() {
        isCancelled.set(true)
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
    }

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    (ipInt shr 8) and 0xff,
                    (ipInt shr 16) and 0xff,
                    (ipInt shr 24) and 0xff
                )
            }
        } catch (_: Exception) {}

        // Fallback interface search
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}

        return "192.168.43.1" // Common hotspot default
    }

    /**
     * Receiver server waiting for files from sender.
     */
    suspend fun startReceiving(
        context: Context,
        port: Int = DEFAULT_PORT,
        onUpdate: (TransferState) -> Unit
    ) = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val targetDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Received")
        if (!targetDir.exists()) targetDir.mkdirs()

        try {
            serverSocket = ServerSocket(port)
            onUpdate(
                TransferState(
                    isRunning = true,
                    isServer = true,
                    statusMessage = "في انتظار اتصال جهاز الإرسال على المنفذ $port..."
                )
            )

            val socket = serverSocket!!.accept()
            activeSocket = socket
            onUpdate(
                TransferState(
                    isRunning = true,
                    isServer = true,
                    statusMessage = "تم الاتصال بجهاز الإرسال! جاري استلام الملفات..."
                )
            )

            val dis = DataInputStream(socket.getInputStream())
            val fileCount = dis.readInt()
            var totalTransferred = 0L

            val buffer = ByteArray(64 * 1024)
            var lastTime = System.currentTimeMillis()
            var bytesSinceLast = 0L

            for (i in 0 until fileCount) {
                if (isCancelled.get()) break
                val fileName = dis.readUTF()
                val fileSize = dis.readLong()

                val destFile = File(targetDir, fileName)
                val fos = FileOutputStream(destFile)

                var fileReceived = 0L
                while (fileReceived < fileSize && !isCancelled.get()) {
                    val toRead = (fileSize - fileReceived).coerceAtMost(buffer.size.toLong()).toInt()
                    val read = dis.read(buffer, 0, toRead)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    fileReceived += read
                    totalTransferred += read
                    bytesSinceLast += read

                    val now = System.currentTimeMillis()
                    val dt = now - lastTime
                    val speed = if (dt >= 500) {
                        val s = (bytesSinceLast * 1000f) / (dt * 1024f * 1024f)
                        bytesSinceLast = 0
                        lastTime = now
                        s
                    } else 0f

                    val prog = if (fileSize > 0) fileReceived.toFloat() / fileSize.toFloat() else 0f
                    onUpdate(
                        TransferState(
                            isRunning = true,
                            isServer = true,
                            currentFileName = fileName,
                            filesSentOrReceived = i + 1,
                            totalFiles = fileCount,
                            bytesTransferred = fileReceived,
                            totalBytes = fileSize,
                            speedMbps = speed,
                            progress = prog,
                            statusMessage = "جاري استلام $fileName (${i + 1}/$fileCount)..."
                        )
                    )
                }
                fos.flush()
                fos.close()
            }

            onUpdate(
                TransferState(
                    isRunning = false,
                    isServer = true,
                    isCompleted = true,
                    statusMessage = "اكتمل استلام جميع الملفات بنجاح في مجلد MaxTools/Received!"
                )
            )
        } catch (e: Exception) {
            if (!isCancelled.get()) {
                onUpdate(
                    TransferState(
                        isRunning = false,
                        error = e.localizedMessage ?: "حدث خطأ أثناء الاستقبال"
                    )
                )
            }
        } finally {
            serverSocket?.close()
            activeSocket?.close()
        }
    }

    /**
     * Sender client connecting to receiver IP & sending selected files.
     */
    suspend fun startSending(
        targetIp: String,
        targetPort: Int = DEFAULT_PORT,
        filesToSend: List<File>,
        onUpdate: (TransferState) -> Unit
    ) = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val validFiles = filesToSend.filter { it.exists() && it.isFile }
        if (validFiles.isEmpty()) {
            onUpdate(TransferState(error = "لم يتم تحديد ملفات صالحة للإرسال."))
            return@withContext
        }

        try {
            onUpdate(TransferState(isRunning = true, statusMessage = "جاري الاتصال بالمستقبل $targetIp..."))
            val socket = Socket(targetIp, targetPort)
            activeSocket = socket
            val dos = DataOutputStream(socket.getOutputStream())

            dos.writeInt(validFiles.size)
            dos.flush()

            val buffer = ByteArray(64 * 1024)
            var lastTime = System.currentTimeMillis()
            var bytesSinceLast = 0L

            for ((index, file) in validFiles.withIndex()) {
                if (isCancelled.get()) break
                dos.writeUTF(file.name)
                val fileSize = file.length()
                dos.writeLong(fileSize)
                dos.flush()

                val fis = FileInputStream(file)
                var fileSent = 0L

                while (fileSent < fileSize && !isCancelled.get()) {
                    val read = fis.read(buffer)
                    if (read == -1) break
                    dos.write(buffer, 0, read)
                    fileSent += read
                    bytesSinceLast += read

                    val now = System.currentTimeMillis()
                    val dt = now - lastTime
                    val speed = if (dt >= 500) {
                        val s = (bytesSinceLast * 1000f) / (dt * 1024f * 1024f)
                        bytesSinceLast = 0
                        lastTime = now
                        s
                    } else 0f

                    val prog = if (fileSize > 0) fileSent.toFloat() / fileSize.toFloat() else 0f
                    onUpdate(
                        TransferState(
                            isRunning = true,
                            currentFileName = file.name,
                            filesSentOrReceived = index + 1,
                            totalFiles = validFiles.size,
                            bytesTransferred = fileSent,
                            totalBytes = fileSize,
                            speedMbps = speed,
                            progress = prog,
                            statusMessage = "جاري إرسال ${file.name} (${index + 1}/${validFiles.size})..."
                        )
                    )
                }
                dos.flush()
                fis.close()
            }

            onUpdate(
                TransferState(
                    isRunning = false,
                    isCompleted = true,
                    statusMessage = "تم إرسال جميع الملفات بنجاح!"
                )
            )
        } catch (e: Exception) {
            if (!isCancelled.get()) {
                onUpdate(
                    TransferState(
                        isRunning = false,
                        error = e.localizedMessage ?: "فشل الاتصال بجهاز الاستقبال"
                    )
                )
            }
        } finally {
            activeSocket?.close()
        }
    }
}
