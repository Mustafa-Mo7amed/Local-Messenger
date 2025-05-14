package com.example.localmessenger

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.*
import java.net.*
import java.util.concurrent.Executors

class ConnectFragment : Fragment(R.layout.fragment_connect) {

    private lateinit var connectionStatus: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnOnOff: Button
    private lateinit var btnDiscover: Button
    private lateinit var lvPeers: ListView
    private lateinit var lvChat: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val messages = mutableListOf<String>()

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private var peers = ArrayList<WifiP2pDevice>()
    private lateinit var deviceArray: Array<WifiP2pDevice>
    private var isHost = false
    private var server: Server? = null
    private var client: Client? = null
    private lateinit var dbHelper: DatabaseHelper
    private var isConnected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())
        
        connectionStatus = view.findViewById(R.id.connection_status)
        etMessage = view.findViewById(R.id.etMessageTesting)
        btnSend = view.findViewById(R.id.btnSendTesting)
        btnOnOff = view.findViewById(R.id.btnOnOff)
        btnDiscover = view.findViewById(R.id.btnDiscover)
        lvPeers = view.findViewById(R.id.lvPeers)
        lvChat = view.findViewById(R.id.lvChat)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messages)
        lvChat.adapter = adapter

        manager = requireContext().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(requireContext(), requireActivity().mainLooper, null)

        receiver = WifiDirectBroadcastReceiver(manager, channel, object : WifiP2pListener {
            override val peerListListener = WifiP2pManager.PeerListListener { peerList ->
                peers.clear()
                peers.addAll(peerList.deviceList)
                deviceArray = peers.toTypedArray()
                val names = peers.map { it.deviceName }.toTypedArray()
                lvPeers.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            }

            override val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
                if (info.groupFormed && info.isGroupOwner) {
                    isHost = true
                    connectionStatus.text = "Host"
                    server = Server()
                    server!!.start()
                } else if (info.groupFormed) {
                    isHost = false
                    connectionStatus.text = "Client"
                    client = Client(info.groupOwnerAddress)
                    client!!.start()
                }
            }

            override fun onP2pStateChanged(enabled: Boolean) {
                connectionStatus.text = if (enabled) "Wi-Fi Direct On" else "Wi-Fi Direct Off"
                isConnected = enabled
            }

            override fun onDisconnected() {
                connectionStatus.text = "Disconnected"
                isConnected = false
                server?.stopServer()
                client?.stopClient()
                server = null
                client = null
            }
        })

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        btnOnOff.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        btnDiscover.setOnClickListener {
            connectionStatus.text = "Attempting Discovery..."
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus.text = "Discovery Started"
                }

                override fun onFailure(reason: Int) {
                    when (reason) {
                        WifiP2pManager.P2P_UNSUPPORTED -> {
                            connectionStatus.text = "P2P not supported on this device"
                        }
                        WifiP2pManager.BUSY -> {
                            connectionStatus.text = "System busy, retrying..."
                            Handler(Looper.getMainLooper()).postDelayed({
                                manager.discoverPeers(channel, this)
                            }, 2000)
                        }
                        WifiP2pManager.ERROR, 0 -> {
                            connectionStatus.text = "Internal error (0). Retrying in 2s..."
                            Handler(Looper.getMainLooper()).postDelayed({
                                manager.discoverPeers(channel, this)
                            }, 2000)
                        }
                        else -> {
                            connectionStatus.text = "Discovery Failed: $reason"
                        }
                    }
                }
            })
        }


        lvPeers.setOnItemClickListener { _, _, i, _ ->
            val device = deviceArray[i]
            val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus.text = "Connecting to ${device.deviceName}"
                }

                override fun onFailure(reason: Int) {
                    connectionStatus.text = "Connect Failed: $reason"
                }
            })
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty()) {
                if (!isConnected) {
                    Toast.makeText(context, "Not connected. Please connect to a peer first.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                messages.add(msg)
                adapter.notifyDataSetChanged()
                lvChat.setSelection(messages.size - 1)
                etMessage.text.clear()

                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    try {
                        if (isHost && server != null) {
                            server?.write(msg.toByteArray())
                            // Save outgoing message for host
                            if (deviceArray.isNotEmpty()) {
                                dbHelper.insertMessage(
                                    deviceArray[0].deviceAddress,
                                    server?.socket?.inetAddress?.hostAddress ?: "unknown",
                                    true,
                                    msg
                                )
                            }
                        } else if (!isHost && client != null) {
                            client?.write(msg.toByteArray())
                            // Save outgoing message for client
                            dbHelper.insertMessage(
                                client?.socket?.localAddress?.hostAddress ?: "unknown",
                                client?.host?.hostAddress ?: "unknown",
                                true,
                                msg
                            )
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Connection not established yet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        checkPermissionsAndDiscover()
    }

    private fun checkPermissionsAndDiscover() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), 101)
        } else {
            btnDiscover.performClick()
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stopServer()
        client?.stopClient()
        if (this::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }

    inner class Server : Thread() {
        private var isRunning = true
        private lateinit var serverSocket: ServerSocket
        internal lateinit var socket: Socket
        private lateinit var outputStream: OutputStream
        private lateinit var inputStream: InputStream
        private var isInitialized = false

        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                socket.keepAlive = true
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()
                isInitialized = true
                isConnected = true

                activity?.runOnUiThread {
                    Toast.makeText(context, "Client connected", Toast.LENGTH_SHORT).show()
                    connectionStatus.text = "Host - Connected"
                }

                val buffer = ByteArray(1024)
                while (isRunning) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            val msg = String(buffer, 0, bytes)
                            activity?.runOnUiThread {
                                if (isAdded && view != null) {
                                    messages.add("Received: $msg")
                                    adapter.notifyDataSetChanged()
                                    lvChat.setSelection(messages.size - 1)
                                    
                                    // Save incoming message for host
                                    dbHelper.insertMessage(
                                        socket.inetAddress.hostAddress,
                                        socket.localAddress.hostAddress,
                                        false,
                                        msg
                                    )
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Error receiving message: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        // Only break if socket is closed or connection is lost
                        if (!socket.isConnected || socket.isClosed) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isInitialized = false
                isConnected = false
                activity?.runOnUiThread {
                    Toast.makeText(context, "Server error: ${e.message}", Toast.LENGTH_SHORT).show()
                    connectionStatus.text = "Host - Error"
                }
            } finally {
                try {
                    if (this::socket.isInitialized) socket.close()
                    if (this::serverSocket.isInitialized) {
                        serverSocket.close()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Server stopped", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun write(data: ByteArray) {
            try {
                if (isInitialized && this::outputStream.isInitialized && socket.isConnected && !socket.isClosed) {
                    outputStream.write(data)
                    outputStream.flush()
                    activity?.runOnUiThread {
                        messages.add("Sent: ${String(data)}")
                        adapter.notifyDataSetChanged()
                        lvChat.setSelection(messages.size - 1)
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Server connection not ready", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun stopServer() {
            isRunning = false
            isInitialized = false
            isConnected = false
            try {
                if (this::socket.isInitialized) socket.close()
                if (this::serverSocket.isInitialized) serverSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            interrupt()
        }
    }


    inner class Client(internal val host: InetAddress) : Thread() {
        private var isRunning = true
        internal lateinit var socket: Socket
        private lateinit var outputStream: OutputStream
        private lateinit var inputStream: InputStream
        private var isInitialized = false

        override fun run() {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host.hostAddress, 8888), 5000)
                socket.keepAlive = true
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()
                isInitialized = true
                isConnected = true

                activity?.runOnUiThread {
                    Toast.makeText(context, "Connected to server", Toast.LENGTH_SHORT).show()
                    connectionStatus.text = "Client - Connected"
                }

                val buffer = ByteArray(1024)
                while (isRunning) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            val msg = String(buffer, 0, bytes)
                            activity?.runOnUiThread {
                                if (isAdded && view != null) {
                                    messages.add("Received: $msg")
                                    adapter.notifyDataSetChanged()
                                    lvChat.setSelection(messages.size - 1)
                                    
                                    // Save incoming message for client
                                    dbHelper.insertMessage(
                                        host.hostAddress,
                                        socket.localAddress.hostAddress,
                                        false,
                                        msg
                                    )
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Error receiving message: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        // Only break if socket is closed or connection is lost
                        if (!socket.isConnected || socket.isClosed) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isInitialized = false
                isConnected = false
                activity?.runOnUiThread {
                    Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    connectionStatus.text = "Client - Error"
                }
            } finally {
                try {
                    if (this::socket.isInitialized) {
                        socket.close()
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Connection closed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun write(data: ByteArray) {
            try {
                if (isInitialized && this::outputStream.isInitialized && socket.isConnected && !socket.isClosed) {
                    outputStream.write(data)
                    outputStream.flush()
                    activity?.runOnUiThread {
                        messages.add("Sent: ${String(data)}")
                        adapter.notifyDataSetChanged()
                        lvChat.setSelection(messages.size - 1)
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Client connection not ready", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun stopClient() {
            isRunning = false
            isInitialized = false
            isConnected = false
            try {
                if (this::socket.isInitialized) {
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            interrupt()
        }
    }

}
