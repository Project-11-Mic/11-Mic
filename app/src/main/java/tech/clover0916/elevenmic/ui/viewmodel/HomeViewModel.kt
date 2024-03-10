package tech.clover0916.elevenmic.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tech.clover0916.elevenmic.server.AudioStreamServer
import java.net.Inet4Address
import java.net.ServerSocket


class HomeViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class HomeViewModel(applicationContext: Context) : ViewModel() {
    private val _connectionStatus = MutableStateFlow("Server Stopped")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    private val audioStreamServer = AudioStreamServer(applicationContext, findAvailablePort())

    private val ipAddressLiveData = findIPAddress(applicationContext)
    val ipAddress: LiveData<String> = ipAddressLiveData

    private fun findAvailablePort(startPort: Int = 8080): Int {
        for (port in startPort..65535) {
            try {
                ServerSocket(port).use { return port }
            } catch (e: Exception) {
                // Port is in use, try the next one
            }
        }
        throw IllegalStateException("No available port found")
    }

    private fun findIPAddress(context: Context): LiveData<String> {
        val liveData = MutableLiveData<String>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val linkProperties = getNetworkLinkProperties(connectivityManager, network)

                // Prioritize IPv4
                val ipv4Address = linkProperties?.linkAddresses?.firstOrNull {
                    it.address is Inet4Address
                }?.address?.hostAddress

                liveData.postValue(ipv4Address ?: "")
            }

            override fun onLost(network: Network) {
                liveData.postValue("")
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        // Also prioritize IPv4 in the initial check
        val currentNetwork = connectivityManager.activeNetwork
        if (currentNetwork != null) {
            val linkProperties = getNetworkLinkProperties(connectivityManager, currentNetwork)

            val ipv4Address = linkProperties?.linkAddresses?.firstOrNull {
                it.address is Inet4Address
            }?.address?.hostAddress

            liveData.postValue(ipv4Address ?: "")
        }

        return liveData
    }
    private fun getNetworkLinkProperties(
        connectivityManager: ConnectivityManager,
        network: Network
    ): LinkProperties? {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            connectivityManager.getLinkProperties(network)
        } else null
    }

    fun toggleAudioStream() {
        if (isServerRunning.value) {
            audioStreamServer.stop(_connectionStatus)
            _isServerRunning.value = false
        } else {
            viewModelScope.launch { // Run in Coroutine scope
                try {
                    audioStreamServer.start(_connectionStatus)
                    _isServerRunning.value = true
                } catch (e: Exception) {
                    _connectionStatus.value = "Error starting server: ${e.message}"
                }
            }
        }
    }
}