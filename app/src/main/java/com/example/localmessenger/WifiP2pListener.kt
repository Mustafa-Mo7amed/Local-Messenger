package com.example.localmessenger

import android.net.wifi.p2p.WifiP2pManager

interface WifiP2pListener {
    val peerListListener: WifiP2pManager.PeerListListener
    val connectionInfoListener: WifiP2pManager.ConnectionInfoListener
    fun onP2pStateChanged(enabled: Boolean)
    fun onDisconnected()
}
