package com.example.localmessenger

import android.Manifest
import android.content.*
import android.net.wifi.p2p.*
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.io.*
import java.net.*
import java.util.concurrent.Executors

class ConnectFragment : Fragment(R.layout.fragment_connect) {

    private lateinit var connectionStatus: TextView
    private lateinit var tvMessage: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnOnOff: Button
    private lateinit var btnDiscover: Button
    private lateinit var listView: ListView

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private var peers = ArrayList<WifiP2pDevice>()
    private lateinit var deviceArray: Array<WifiP2pDevice>
    private var isHost = false
    private var server: Server? = null
    private var client: Client? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectionStatus = view.findViewById(R.id.connection_status)
        tvMessage = view.findViewById(R.id.tvMessageTesting)
        etMessage = view.findViewById(R.id.etMessageTesting)
        btnSend = view.findViewById(R.id.btnSendTesting)
        btnOnOff = view.findViewById(R.id.btnOnOff)
        btnDiscover = view.findViewById(R.id.btnDiscover)
        listView = view.findViewById(R.id.lvPeers)

        manager = requireContext().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(requireContext(), requireActivity().mainLooper, null)

        receiver = WifiDirectBroadcastReceiver(manager, channel, object : WifiP2pListener {
            override val peerListListener = WifiP2pManager.PeerListListener { peerList ->
                peers.clear()
                peers.addAll(peerList.deviceList)
                deviceArray = peers.toTypedArray()
                val names = peers.map { it.deviceName }.toTypedArray()
                listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
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
            }

            override fun onDisconnected() {
                connectionStatus.text = "Disconnected"
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
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus.text = "Discovery Started"
                }

                override fun onFailure(reason: Int) {
                    connectionStatus.text = "Discovery Failed: $reason"
                }
            })
        }

        listView.setOnItemClickListener { _, _, i, _ ->
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
                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    if (isHost) server?.write(msg.toByteArray())
                    else client?.write(msg.toByteArray())
                }
            }
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

    inner class Server : Thread() {
        private lateinit var socket: Socket
        private lateinit var outputStream: OutputStream
        private lateinit var inputStream: InputStream

        override fun run() {
            try {
                val serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()

                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val msg = String(buffer, 0, bytes)
                        Handler(Looper.getMainLooper()).post {
                            tvMessage.text = msg
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun write(data: ByteArray) {
            try {
                outputStream.write(data)
                outputStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    inner class Client(private val host: InetAddress) : Thread() {
        private lateinit var socket: Socket
        private lateinit var outputStream: OutputStream
        private lateinit var inputStream: InputStream

        override fun run() {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host.hostAddress, 8888), 500)
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()

                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val msg = String(buffer, 0, bytes)
                        Handler(Looper.getMainLooper()).post {
                            tvMessage.text = msg
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun write(data: ByteArray) {
            try {
                outputStream.write(data)
                outputStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
