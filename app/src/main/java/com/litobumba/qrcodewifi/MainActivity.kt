package com.litobumba.qrcodewifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.litobumba.qrcodewifi.ui.theme.QRCodeWifiTheme

class MainActivity : ComponentActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private var hasScanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRCodeWifiTheme {

                wifiManager = remember {
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                }

                connectivityManager = remember {
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionRequest(
                        onPermissionPermanentlyDenied = {
                            PermissionDeniedDialog(isPermanently = true)
                        },
                        onPermissionDenied = {
                            PermissionDeniedDialog(isPermanently = false)
                        }
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { ssid, pw ->
                                if (!hasScanned) {
                                    hasScanned = true
                                    is29AndAbove {
                                        connect29AndAbove(
                                            ssid = ssid,
                                            passPhrase = pw
                                        )
                                    } ?: connectBelow29(ssid = ssid, pw)
                                }
                            },
                            onFailure = {
                                if (!hasScanned) {
                                    hasScanned = true
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Error's occurred ${it.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connect29AndAbove(ssid: String, passPhrase: String) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Wifi is disabled please enable it to proceed", Toast.LENGTH_SHORT).show()
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            launcher.launch(panelIntent)
        }
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passPhrase)
            .setIsAppInteractionRequired(true)
            .build()

        wifiManager.removeNetworkSuggestions(listOf(suggestion))

        wifiManager.addNetworkSuggestions(listOf(suggestion))

        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
            }
        }
        this.registerReceiver(broadcastReceiver, intentFilter)
        hasScanned = false
    }

    @SuppressLint("MissingPermission")
    fun connectBelow29(ssid: String, passPhrase: String) {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Enabling wifi...", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true
        }

        val conf = WifiConfiguration()
        conf.SSID = String.format("\"%s\"", ssid)
        conf.preSharedKey = String.format("\"%s\"", passPhrase)
        wifiManager.addNetwork(conf)
        val netId = wifiManager.addNetwork(conf)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
        hasScanned = false
    }

}