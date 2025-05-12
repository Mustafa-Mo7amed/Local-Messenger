package com.example.localmessenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager

class WifiDirectBroadcastReceiver(
    private var manager: WifiP2pManager?,
    private var channel: WifiP2pManager.Channel,
    private var activity: MessageActivity
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var action = intent?.action
        if (action == WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION) {

        }
        else if (action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
//            manager?.requestPeers(channel, activity.peerListListener)
        }
        else if (action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
//            val networkInfo = intent?.getParcelableExtra<android.net.NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
//            if (networkInfo != null && networkInfo.isConnected) {
//                manager?.requestConnectionInfo(channel, activity.connectionInfoListener)
//            }
//            else {
//                activity.connectionStatus.text = "Disconnected"
//            }
        }
    }

}