package com.example.smartassistivestick.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED
    }

    companion object {
        private const val TAG = "BT"
        private const val TARGET_DEVICE_NAME = "HC-05"
        private const val READ_START_DELAY_MS = 500L
        private const val RECONNECT_DELAY_MS = 2000L
    }

    private val bluetoothAdapter: BluetoothAdapter? = android.bluetooth.BluetoothManager::class.java.let {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    }

    private val lock = Any()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var connectThread: Thread? = null
    private var readThread: Thread? = null

    @Volatile
    private var shouldAutoReconnect = false

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _incomingMessages = MutableStateFlow("")
    val incomingMessages: StateFlow<String> = _incomingMessages

    // Standard SPP UUID
    private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connectToDevice(deviceAddress: String? = null) {
        synchronized(lock) {
            shouldAutoReconnect = true
            connectThread?.interrupt()
            connectThread = Thread {
                try {
                    connectInternal(deviceAddress)
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    onConnectionFailed(deviceAddress, e)
                }
            }.apply { start() }
        }
    }

    private fun connectInternal(deviceAddress: String?) {
        val adapter = bluetoothAdapter ?: throw IOException("Bluetooth adapter is not available")

        updateStatus(ConnectionStatus.CONNECTING)
        Log.d(TAG, "Trying to connect")
        adapter.cancelDiscovery()

        // Ensure only one active connection at a time.
        closeActiveConnection()

        val device = adapter.bondedDevices.firstOrNull { it.name == TARGET_DEVICE_NAME }
            ?: deviceAddress?.let {
                runCatching { adapter.getRemoteDevice(it) }.getOrNull()
            }
            ?: throw IOException("Paired device $TARGET_DEVICE_NAME not found")

        val connectedSocket = connectWithFallback(device)
        synchronized(lock) {
            socket = connectedSocket
            inputStream = connectedSocket.inputStream
        }

        updateStatus(ConnectionStatus.CONNECTED)
        Log.d(TAG, "Connected successfully")

        Thread.sleep(READ_START_DELAY_MS)
        startListening(connectedSocket, deviceAddress)
    }

    private fun connectWithFallback(device: BluetoothDevice): BluetoothSocket {
        val primarySocket = try {
            device.createRfcommSocketToServiceRecord(myUUID)
        } catch (e: Exception) {
            Log.d(TAG, "Primary socket creation failed, using fallback")
            createFallbackSocket(device)
        }

        try {
            primarySocket.connect()
            return primarySocket
        } catch (e: IOException) {
            Log.e(TAG, "Primary connect failed, trying fallback", e)
            runCatching { primarySocket.close() }
            val fallbackSocket = createFallbackSocket(device)
            fallbackSocket.connect()
            return fallbackSocket
        }
    }

    private fun createFallbackSocket(device: BluetoothDevice): BluetoothSocket {
        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        return method.invoke(device, 1) as BluetoothSocket
    }

    private fun startListening(activeSocket: BluetoothSocket, deviceAddress: String?) {
        synchronized(lock) {
            readThread?.interrupt()
            readThread = Thread {
                val buffer = ByteArray(1024)
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val bytes = activeSocket.inputStream.read(buffer)
                        if (bytes <= 0) {
                            continue
                        }
                        val message = String(buffer, 0, bytes).trim()
                        if (message.isNotEmpty()) {
                            _incomingMessages.value = message
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Connection failed", e)
                        onConnectionFailed(deviceAddress, e)
                        break
                    }
                }
            }.apply { start() }
        }
    }

    private fun onConnectionFailed(deviceAddress: String?, error: Exception) {
        val retryEnabled = shouldAutoReconnect
        closeActiveConnection()

        if (!retryEnabled && _connectionStatus.value == ConnectionStatus.DISCONNECTED) {
            return
        }

        updateStatus(ConnectionStatus.FAILED)

        if (retryEnabled) {
            try {
                Thread.sleep(RECONNECT_DELAY_MS)
                connectToDevice(deviceAddress)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e(TAG, "Reconnect interrupted", interrupted)
            }
        } else {
            Log.e(TAG, "Connection failed", error)
        }
    }

    private fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
        _connectionState.value = status == ConnectionStatus.CONNECTED
    }

    private fun closeActiveConnection() {
        synchronized(lock) {
            runCatching { inputStream?.close() }
            runCatching { socket?.close() }
            inputStream = null
            socket = null
            readThread?.interrupt()
            readThread = null
        }
    }

    fun disconnect() {
        shouldAutoReconnect = false
        updateStatus(ConnectionStatus.DISCONNECTED)
        connectThread?.interrupt()
        closeActiveConnection()
    }
}
