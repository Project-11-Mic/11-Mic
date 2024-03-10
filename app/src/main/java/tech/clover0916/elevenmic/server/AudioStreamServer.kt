package tech.clover0916.elevenmic.server

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

class AudioStreamServer(private val context: Context, private val port: Int) {

    private var serverSocket: ServerSocket? = null
    private var audioRecorder: AudioRecord? = null

    fun start(connectionStatus: MutableStateFlow<String>) {
        serverSocket = ServerSocket(port)
        println("Server started on port $port")

        connectionStatus.value = "Waiting for client connection"

        Thread {
            while (true) {
                try {
                    val client = serverSocket?.accept()
                    client?.let {
                        println("Client connected: ${it.inetAddress.hostAddress}")
                        connectionStatus.value = "Client connected: ${it.inetAddress.hostAddress}"
                        handleClientConnection(it, connectionStatus)
                    }
                } catch (e: Exception) {
                    println("Error handling connection: ${e.message}")
                    break // Handle server shutdown and error appropriately
                }
            }
        }.start()
    }

    fun stop(connectionStatus: MutableStateFlow<String>) {
        serverSocket?.close()
        audioRecorder?.stop()
        audioRecorder?.release()
        println("Server stopped")
        connectionStatus.value = "Server Stopped"
    }

    private fun handleClientConnection(client: Socket, connectionStatus: MutableStateFlow<String>) {
        try {
            // Configure and start AudioRecord instance (sample rate, format, ...)
            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 2

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                audioRecorder?.startRecording()
            } else {
                // Handle the case when the permission is not granted
            }

            val outputStream = DataOutputStream(client.getOutputStream())

            // Read audio data from recorder and stream over the socket
            val buffer = ByteArray(bufferSize)
            while (!client.isClosed) {
                val bytesRead = audioRecorder?.read(buffer, 0, buffer.size) ?: break
                outputStream.write(buffer, 0, bytesRead)
            }

        } catch (e: Exception) {
            println("Error handling client connection: ${e.message}")
        } finally {
            client.close()
            audioRecorder?.stop()
            audioRecorder?.release()
            connectionStatus.value = "Waiting for connection"
        }
    }
}