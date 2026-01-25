package org.cosmic.cosmicconnect.UserInterface

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Device
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: CosmicConnect
) : ViewModel() {

    private val _selectedDeviceId = MutableLiveData<String?>()
    val selectedDeviceId: LiveData<String?> = _selectedDeviceId

    private val _deviceList = MutableLiveData<List<Device>>()
    val deviceList: LiveData<List<Device>> = _deviceList

    init {
        app.addDeviceListChangedCallback("MainViewModel") {
            updateDeviceList()
        }
        updateDeviceList()
    }

    private fun updateDeviceList() {
        val devices = app.devices.values.toList()
        _deviceList.postValue(devices)
    }

    fun selectDevice(deviceId: String?) {
        _selectedDeviceId.value = deviceId
    }

    override fun onCleared() {
        super.onCleared()
        app.removeDeviceListChangedCallback("MainViewModel")
    }
}
