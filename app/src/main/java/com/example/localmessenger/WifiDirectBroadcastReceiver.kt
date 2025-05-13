package com.example.localmessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel,
    private val listener: WifiP2pListener    // listener to push events back
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {

            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager?.requestPeers(channel, listener.peerListListener)
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // request connection info when a real connection is made
                val netInfo = intent
                    .getParcelableExtra<android.net.NetworkInfo>(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                    )
                if (netInfo?.isConnected == true) {
                    // call fragment connectionInfoListener
                    manager?.requestConnectionInfo(channel, listener.connectionInfoListener)
                } else {
                    listener.onDisconnected()
                }
            }
        }
    }
}

