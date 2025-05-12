package com.example.localmessenger

import android.Manifest                                        // ▶ Imported Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager                         // ▶ Imported PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build                                           // ▶ Imported Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat                         // ▶ Ensure ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat                           // ▶ Imported ActivityCompat for fragment requestPermissions
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ConnectFragment : Fragment(R.layout.fragment_connect), WifiP2pListener {

    // UI
    private lateinit var connectionStatus: TextView
    private lateinit var btnOnOff: Button
    private lateinit var btnDiscover: Button
    private lateinit var lvPeers: ListView

    // P2P
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    // Peer list
    private var peers = ArrayList<WifiP2pDevice>()
    private lateinit var deviceNameArray: Array<String>
    private lateinit var deviceArray: Array<WifiP2pDevice>

    // Networking threads
    private lateinit var server: Server
    private lateinit var client: Client

    // Whether we can now send
    private var canSend = false

    companion object {
        private const val REQUEST_LOCATION = 100
        private const val REQUEST_NEARBY  = 101
    }

    // ▶ MODIFIED: callbacks for socket events
    private val onConnectedCallback: () -> Unit = {
        requireActivity().runOnUiThread {
            canSend = true
            connectionStatus.text = "Ready to send"
        }
    }

    private val onMessageCallback: (String) -> Unit = { msg ->
        val bundle = Bundle().apply {
            putString("text", msg)
            putBoolean("incoming", true)
        }
        parentFragmentManager.setFragmentResult("chat_message", bundle)
    }

    // -------------------------------
    // Fragment & P2P Listener implementations
    // -------------------------------
    override val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        // ▶ MODIFIED: Re-enable discover button once peers update
        btnDiscover.isEnabled = true
        if (peerList.deviceList == peers) return@PeerListListener
        peers.clear()
        peers.addAll(peerList.deviceList)

        deviceNameArray = peers.map { it.deviceName }.toTypedArray()
        deviceArray = peers.toTypedArray()

        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1,
            deviceNameArray)
        lvPeers.adapter = adapter

        if (peers.isEmpty()) {
            connectionStatus.text = "No Devices Found"
        }
    }

    override val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        if (info.groupFormed && info.isGroupOwner) {
            connectionStatus.text = "Host"
            startServer()
        } else {
            connectionStatus.text = "Client"
            startClient(info.groupOwnerAddress)
        }
    }

    override fun onP2pStateChanged(enabled: Boolean) {
        connectionStatus.text = if (enabled) "P2P is ON" else "P2P is OFF"
    }

    override fun onDisconnected() {
        connectionStatus.text = "Disconnected"
        canSend = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(view)

        // ▶ MODIFIED: setup onOff and permissions + auto-discover
        btnOnOff.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        checkPermissionsAndDiscover()   // ▶ MODIFIED: request and then discover
    }

    private fun initialize(view: View) {
        connectionStatus = view.findViewById(R.id.connection_status)
        btnOnOff = view.findViewById(R.id.btnOnOff)
        btnDiscover = view.findViewById(R.id.btnDiscover)
        lvPeers = view.findViewById(R.id.lvPeers)

        manager = requireContext().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(requireContext(), requireContext().mainLooper, null)

        receiver = WifiDirectBroadcastReceiver(manager, channel, this)
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        btnDiscover.setOnClickListener {
            btnDiscover.isEnabled = false    // ▶ MODIFIED: disable button while discovering
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus.text = "Discovery Started"
                }
                override fun onFailure(reason: Int) {
                    // ▶ MODIFIED: detailed failure reasons
                    when (reason) {
                        WifiP2pManager.ERROR ->
                            connectionStatus.text = "Internal error—try again"
                        WifiP2pManager.P2P_UNSUPPORTED ->
                            connectionStatus.text = "P2P not supported"
                        WifiP2pManager.BUSY ->
                            connectionStatus.text = "Framework busy—retry later"
                        else ->
                            connectionStatus.text = "Discovery Failed: $reason"
                    }
                    btnDiscover.isEnabled = true  // re-enable on failure
                }
            })
        }

        lvPeers.setOnItemClickListener { _, _, pos, _ ->
            val device = deviceArray[pos]
            val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus.text = "Connecting..."
                }
                override fun onFailure(reason: Int) {
                    connectionStatus.text = "Connect Failed: $reason"
                }
            })
        }
    }

    // ▶ MODIFIED: Permission request & discovery chaining
    private fun checkPermissionsAndDiscover() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), REQUEST_NEARBY)
        } else {
            btnDiscover.performClick()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == REQUEST_LOCATION || requestCode == REQUEST_NEARBY)
            && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionsAndDiscover()
        } else {
            connectionStatus.text = "Permission required to discover peers"
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

    private fun startClient(host: InetAddress) {
        client = Client(host, onConnectedCallback, onMessageCallback)
        client.start()
    }

    private fun startServer() {
        server = Server(onConnectedCallback, onMessageCallback)
        server.start()
    }

    // ▶ MODIFIED: thread definitions use callbacks
    class Client(
        private val host: InetAddress,
        private val onConnected: () -> Unit,
        private val onMessage: (String) -> Unit
    ) : Thread() {
        private var socket: Socket? = null
        override fun run() {
            try {
                socket = Socket().apply { connect(InetSocketAddress(host.hostAddress, 8888), 500) }
                onConnected()
                val input = socket!!.getInputStream()
                val buf = ByteArray(1024)
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    onMessage(String(buf, 0, read))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        fun write(bytes: ByteArray) {
            try { socket?.getOutputStream()?.apply { write(bytes); flush() } } catch (e: Exception) { e.printStackTrace() }
        }
    }

    class Server(
        private val onConnected: () -> Unit,
        private val onMessage: (String) -> Unit
    ) : Thread() {
        private var serverSocket: ServerSocket? = null
        private var clientSocket: Socket? = null
        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                clientSocket = serverSocket!!.accept()
                onConnected()
                val input = clientSocket!!.getInputStream()
                val buf = ByteArray(1024)
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    onMessage(String(buf, 0, read))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        fun write(bytes: ByteArray) { try { clientSocket?.getOutputStream()?.apply { write(bytes); flush() } } catch (e: Exception) { e.printStackTrace() } }
    }
}
