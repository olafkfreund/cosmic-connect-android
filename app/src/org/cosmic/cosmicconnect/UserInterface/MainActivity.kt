/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.UserInterface

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.ArrayUtils
import org.cosmic.cosmicconnect.BackgroundService
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareSettingsFragment
import org.cosmic.cosmicconnect.UserInterface.About.AboutFragment
import org.cosmic.cosmicconnect.UserInterface.About.getApplicationAboutData
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

private const val MENU_ENTRY_ADD_DEVICE = 1 //0 means no-selection
private const val MENU_ENTRY_SETTINGS = 2
private const val MENU_ENTRY_ABOUT = 3
private const val MENU_ENTRY_DEVICE_FIRST_ID = 1000 //All subsequent ids are devices in the menu
private const val MENU_ENTRY_DEVICE_UNKNOWN = 9999 //It's still a device, but we don't know which one yet
private const val STORAGE_LOCATION_CONFIGURED = 2020
private const val STATE_SELECTED_MENU_ENTRY = "selected_entry" //Saved only in onSaveInstanceState
private const val STATE_SELECTED_DEVICE = "selected_device" //Saved persistently in preferences

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mNavigationView: NavigationView by lazy { binding.navigationDrawer }
    private var mDrawerLayout: DrawerLayout? = null

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    private lateinit var mNavViewDeviceName: TextView

    private var mCurrentDevice: String? = null
    private var mCurrentMenuEntry = 0
    private val preferences: SharedPreferences by lazy { getSharedPreferences("stored_menu_selection", MODE_PRIVATE) }
    private val mMapMenuToDeviceId = HashMap<MenuItem, String>()

    private val storageLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                ShareSettingsFragment.saveStorageLocationPreference(this, uri)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && results[Manifest.permission.POST_NOTIFICATIONS] == true) {
            // If PairingFragment is active, reload it
            if (mCurrentDevice == null) {
                navController.navigate(R.id.pairingFragment)
            }
        }
    }

    private val closeDrawerCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            mDrawerLayout?.closeDrawer(mNavigationView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceHelper.initializeDeviceId(this)

        val root = binding.root
        setContentView(root)
        mDrawerLayout = root as? DrawerLayout

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            mCurrentMenuEntry = when (destination.id) {
                R.id.pairingFragment -> MENU_ENTRY_ADD_DEVICE
                R.id.settingsFragment -> MENU_ENTRY_SETTINGS
                R.id.aboutFragment -> MENU_ENTRY_ABOUT
                R.id.deviceFragment -> {
                    val deviceId = arguments?.getString("deviceId")
                    deviceIdToMenuEntryId(deviceId)
                }
                else -> MENU_ENTRY_DEVICE_UNKNOWN
            }
            if (mCurrentMenuEntry != MENU_ENTRY_DEVICE_UNKNOWN) {
                mNavigationView.setCheckedItem(mCurrentMenuEntry)
            } else {
                 uncheckAllMenuItems(mNavigationView.menu)
            }
            
             if (destination.id == R.id.deviceFragment) {
                 mCurrentDevice = arguments?.getString("deviceId")
            } else {
                 mCurrentDevice = null
            }
            preferences.edit { putString(STATE_SELECTED_DEVICE, mCurrentDevice) }
        }

        val mDrawerHeader = mNavigationView.getHeaderView(0)
        mNavViewDeviceName = mDrawerHeader.findViewById(R.id.device_name)
        val mNavViewDeviceType = mDrawerHeader.findViewById<ImageView>(R.id.device_type)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        mDrawerLayout?.let {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            val mDrawerToggle = DrawerToggle(it).apply { syncState() }
            it.addDrawerListener(mDrawerToggle)
            it.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        } ?: {
            supportActionBar?.setDisplayShowHomeEnabled(false)
            supportActionBar?.setHomeButtonEnabled(false)
        }

        // Note: The preference changed listener should be registered before getting the name, because getting
        // it can trigger a background fetch from the internet that will eventually update the preference
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
        val deviceName = DeviceHelper.getDeviceName(this)
        mNavViewDeviceType?.setImageDrawable(DeviceHelper.deviceType.getIcon(this))
        mNavViewDeviceName.text = deviceName
        mNavigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            mCurrentMenuEntry = menuItem.itemId
            when (mCurrentMenuEntry) {
                MENU_ENTRY_ADD_DEVICE -> {
                    mCurrentDevice = null
                    preferences.edit { putString(STATE_SELECTED_DEVICE, null) }
                    navController.navigate(R.id.pairingFragment)
                }

                MENU_ENTRY_SETTINGS -> {
                    preferences.edit { putString(STATE_SELECTED_DEVICE, null) }
                    navController.navigate(R.id.settingsFragment)
                }

                MENU_ENTRY_ABOUT -> {
                    preferences.edit { putString(STATE_SELECTED_DEVICE, null) }
                    val args = Bundle().apply { putParcelable("about_data", getApplicationAboutData(this@MainActivity)) }
                    navController.navigate(R.id.aboutFragment, args)
                }

                else -> {
                    val deviceId = mMapMenuToDeviceId[menuItem]
                    onDeviceSelected(deviceId)
                }
            }
            mDrawerLayout?.closeDrawer(mNavigationView)
            true
        }

        // Decide which menu entry should be selected at start
        var savedDevice: String?
        var savedMenuEntry: Int
        when {
            intent.hasExtra(FLAG_FORCE_OVERVIEW) -> {
                Log.i(this::class.simpleName, "Requested to start main overview")
                savedDevice = null
                savedMenuEntry = MENU_ENTRY_ADD_DEVICE
            }

            intent.hasExtra(EXTRA_DEVICE_ID) -> {
                Log.i(this::class.simpleName, "Loading selected device from parameter")
                savedDevice = intent.getStringExtra(EXTRA_DEVICE_ID)
                savedMenuEntry = MENU_ENTRY_DEVICE_UNKNOWN
                // If pairStatus is not empty, then the user has accepted/reject the pairing from the notification
                val pairStatus = intent.getStringExtra(PAIR_REQUEST_STATUS)
                if (pairStatus != null) {
                    Log.i(this::class.simpleName, "Pair status is $pairStatus")
                    savedDevice = onPairResultFromNotification(savedDevice, pairStatus)
                    if (savedDevice == null) {
                        savedMenuEntry = MENU_ENTRY_ADD_DEVICE
                    }
                }
            }

            savedInstanceState != null -> {
                Log.i(this::class.simpleName, "Loading selected device from saved activity state")
                savedDevice = savedInstanceState.getString(STATE_SELECTED_DEVICE)
                savedMenuEntry = savedInstanceState.getInt(STATE_SELECTED_MENU_ENTRY, MENU_ENTRY_ADD_DEVICE)
            }

            else -> {
                Log.i(this::class.simpleName, "Loading selected device from persistent storage")
                savedDevice = preferences.getString(STATE_SELECTED_DEVICE, null)
                savedMenuEntry = if (savedDevice != null) MENU_ENTRY_DEVICE_UNKNOWN else MENU_ENTRY_ADD_DEVICE
            }
        }
        mCurrentMenuEntry = savedMenuEntry
        mCurrentDevice = savedDevice
        mNavigationView.setCheckedItem(savedMenuEntry)

        if (savedInstanceState == null) {
            // Activate the chosen fragment and select the entry in the menu
            if (savedMenuEntry >= MENU_ENTRY_DEVICE_FIRST_ID && savedDevice != null) {
                onDeviceSelected(savedDevice)
            } else {
                when (mCurrentMenuEntry) {
                    MENU_ENTRY_SETTINGS -> navController.navigate(R.id.settingsFragment)
                    MENU_ENTRY_ABOUT -> {
                        val args = Bundle().apply { putParcelable("about_data", getApplicationAboutData(this@MainActivity)) }
                        navController.navigate(R.id.aboutFragment, args)
                    }
                    else -> navController.navigate(R.id.pairingFragment)
                }
            }
        }

        val missingPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if(missingPermissions.isNotEmpty()){
            notificationPermissionLauncher.launch(missingPermissions.toTypedArray())
        }

        viewModel.deviceList.observe(this) { devices ->
            updateDeviceList(devices)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun onPairResultFromNotification(deviceId: String?, pairStatus: String): String? {
        assert(deviceId != null)
        if (pairStatus != PAIRING_PENDING) {
            val device = CosmicConnect.getInstance().getDevice(deviceId)
            if (device == null) {
                Log.w(this::class.simpleName, "Reject pairing - device no longer exists: $deviceId")
                return null
            }
            when (pairStatus) {
                PAIRING_ACCEPTED -> device.acceptPairing()
                PAIRING_REJECTED -> device.cancelPairing()
            }
        }
        return if (pairStatus == PAIRING_ACCEPTED || pairStatus == PAIRING_PENDING) deviceId else null
    }

    private fun deviceIdToMenuEntryId(deviceId: String?): Int {
        for ((key, value) in mMapMenuToDeviceId) {
            if (value == deviceId) {
                return key.itemId
            }
        }
        return MENU_ENTRY_DEVICE_UNKNOWN
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            mDrawerLayout?.openDrawer(mNavigationView)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateDeviceList(devices: Collection<Device> = emptyList()) {
        val menu = mNavigationView.menu
        menu.clear()
        mMapMenuToDeviceId.clear()
        val devicesMenu = menu.addSubMenu(R.string.devices)
        var id = MENU_ENTRY_DEVICE_FIRST_ID
        for (device in devices) {
            if (device.isReachable && device.isPaired) {
                val item = devicesMenu.add(Menu.FIRST, id++, 1, device.name)
                item.icon = device.icon
                item.isCheckable = true
                mMapMenuToDeviceId[item] = device.deviceId
            }
        }
        val addDeviceItem = devicesMenu.add(Menu.FIRST, MENU_ENTRY_ADD_DEVICE, 1000, R.string.pair_new_device)
        addDeviceItem.setIcon(R.drawable.ic_action_content_add_circle_outline_32dp)
        addDeviceItem.isCheckable = true
        val settingsItem = menu.add(Menu.FIRST, MENU_ENTRY_SETTINGS, 1000, R.string.settings)
        settingsItem.setIcon(R.drawable.ic_settings_white_32dp)
        settingsItem.isCheckable = true
        val aboutItem = menu.add(Menu.FIRST, MENU_ENTRY_ABOUT, 1000, R.string.about)
        aboutItem.setIcon(R.drawable.ic_baseline_info_24)
        aboutItem.isCheckable = true

        //Ids might have changed
        if (mCurrentMenuEntry >= MENU_ENTRY_DEVICE_FIRST_ID) {
            mCurrentMenuEntry = deviceIdToMenuEntryId(mCurrentDevice)
        }
        mNavigationView.setCheckedItem(mCurrentMenuEntry)
    }

    override fun onStart() {
        super.onStart()
        BackgroundService.Start(applicationContext)
        // CosmicConnect.getInstance().addDeviceListChangedCallback(this::class.simpleName!!) { runOnUiThread { updateDeviceList() } } // Handled by ViewModel
        // updateDeviceList() // Handled by ViewModel observation
        onBackPressedDispatcher.addCallback(closeDrawerCallback)
        if (mDrawerLayout == null) closeDrawerCallback.isEnabled = false
    }

    override fun onStop() {
        // CosmicConnect.getInstance().removeDeviceListChangedCallback(this::class.simpleName!!) // Handled by ViewModel
        closeDrawerCallback.remove()
        super.onStop()
    }

    @JvmOverloads
    fun onDeviceSelected(deviceId: String?, fromDeviceList: Boolean = false) {
        mCurrentDevice = deviceId
        preferences.edit { putString(STATE_SELECTED_DEVICE, deviceId) }
        if (mCurrentDevice != null) {
            mCurrentMenuEntry = deviceIdToMenuEntryId(deviceId)
            if (mCurrentMenuEntry == MENU_ENTRY_DEVICE_UNKNOWN) {
                uncheckAllMenuItems(mNavigationView.menu)
            } else {
                mNavigationView.setCheckedItem(mCurrentMenuEntry)
            }
            val args = Bundle().apply {
                putString("deviceId", deviceId)
                putBoolean("fromDeviceList", fromDeviceList)
            }
            navController.navigate(R.id.deviceFragment, args)
        } else {
            mCurrentMenuEntry = MENU_ENTRY_ADD_DEVICE
            mNavigationView.setCheckedItem(mCurrentMenuEntry)
            navController.navigate(R.id.pairingFragment)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_DEVICE, mCurrentDevice)
        outState.putInt(STATE_SELECTED_MENU_ENTRY, mCurrentMenuEntry)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == RESULT_NEEDS_RELOAD -> {
                CoroutineScope(Dispatchers.IO).launch {
                    CosmicConnect.getInstance().devices.values.forEach(Device::reloadPluginsFromSettings)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun isPermissionGranted(permissions: Array<String>, grantResults: IntArray, permission : String) : Boolean {
        val index = ArrayUtils.indexOf(permissions, permission)
        return index != ArrayUtils.INDEX_NOT_FOUND && grantResults[index] == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsGranted = ArrayUtils.contains(grantResults, PackageManager.PERMISSION_GRANTED)
        if (permissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isPermissionGranted(permissions, grantResults, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // To get a writeable path manually on Android 10 and later for Share and Receive Plugin.
                // Otherwise, Receiving files will keep failing until the user chooses a path manually to receive files.
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                storageLocationLauncher.launch(intent)
            }

            if (isPermissionGranted(permissions, grantResults, Manifest.permission.BLUETOOTH_CONNECT) &&
                isPermissionGranted(permissions, grantResults, Manifest.permission.BLUETOOTH_SCAN)) {
                PreferenceManager.getDefaultSharedPreferences(this).edit {
                    putBoolean(SettingsFragment.KEY_BLUETOOTH_ENABLED, true)
                }
                navController.navigate(R.id.settingsFragment)
            }

            //New permission granted, reload plugins
            CoroutineScope(Dispatchers.IO).launch {
                CosmicConnect.getInstance().devices.values.forEach(Device::reloadPluginsFromSettings)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (DeviceHelper.KEY_DEVICE_NAME_PREFERENCE == key) {
            mNavViewDeviceName.text = DeviceHelper.getDeviceName(this)
            BackgroundService.ForceRefreshConnections(this) //Re-send our identity packet
        }
    }

    private fun uncheckAllMenuItems(menu: Menu) {
        val size = menu.size
        for (i in 0 until size) {
            val item = menu[i]
            item.subMenu?.let { uncheckAllMenuItems(it) } ?: item.setChecked(false)
        }
    }

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"
        const val PAIR_REQUEST_STATUS = "pair_req_status"
        const val PAIRING_ACCEPTED = "accepted"
        const val PAIRING_REJECTED = "rejected"
        const val PAIRING_PENDING = "pending"
        const val RESULT_NEEDS_RELOAD = RESULT_FIRST_USER
        const val RESULT_NOTIFICATIONS_ENABLED = RESULT_FIRST_USER+1
        const val FLAG_FORCE_OVERVIEW = "forceOverview"
    }

    private inner class DrawerToggle(drawerLayout: DrawerLayout) : ActionBarDrawerToggle(
        this,  /* host Activity */
        drawerLayout,  /* DrawerLayout object */
        R.string.open,  /* "open drawer" description */
        R.string.close /* "close drawer" description */
    ) {
        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            closeDrawerCallback.isEnabled = false
        }

        override fun onDrawerOpened(drawerView: View) {
            super.onDrawerOpened(drawerView)
            closeDrawerCallback.isEnabled = true
        }
    }

}
