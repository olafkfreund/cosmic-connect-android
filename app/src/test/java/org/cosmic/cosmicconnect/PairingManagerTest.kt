/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import android.content.Context
import io.mockk.mockk
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.PairingHandler.PairingCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.cert.Certificate

@RunWith(RobolectricTestRunner::class)
class PairingManagerTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var sslHelper: SslHelper
    private var pluginsReloaded = false
    private var unpairedDeviceId: String? = null

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        device = mockk(relaxed = true)
        sslHelper = mockk(relaxed = true)
        pluginsReloaded = false
        unpairedDeviceId = null

        val mockCert = mockk<Certificate>(relaxed = true)
        deviceInfo = DeviceInfo(
            id = "test-device-id",
            certificate = mockCert,
            name = "Test Device",
            type = DeviceType.PHONE
        )
    }

    private fun createManager(initialState: PairingHandler.PairState = PairingHandler.PairState.NotPaired): PairingManager {
        return PairingManager(
            context = context,
            device = device,
            deviceInfo = deviceInfo,
            sslHelper = sslHelper,
            onPluginsReload = { pluginsReloaded = true },
            onPluginsUnpaired = { _, id -> unpairedDeviceId = id },
            initialState = initialState
        )
    }

    @Test
    fun `isPaired returns true when state is Paired`() {
        val manager = createManager(PairingHandler.PairState.Paired)
        assertTrue(manager.isPaired)
    }

    @Test
    fun `isPaired returns false when state is NotPaired`() {
        val manager = createManager(PairingHandler.PairState.NotPaired)
        assertFalse(manager.isPaired)
    }

    @Test
    fun `pairStatus reflects initial state`() {
        val manager = createManager(PairingHandler.PairState.Requested)
        assertEquals(PairingHandler.PairState.Requested, manager.pairStatus)
    }

    @Test
    fun `pairingHandler is created and accessible`() {
        val manager = createManager()
        assertNotNull(manager.pairingHandler)
    }

    @Test
    fun `addPairingCallback and removePairingCallback work`() {
        val manager = createManager()
        var callbackCalled = false
        val callback = object : PairingCallback {
            override fun incomingPairRequest() { callbackCalled = true }
            override fun pairingFailed(error: String) {}
            override fun pairingSuccessful() {}
            override fun unpaired(device: Device) {}
        }

        manager.addPairingCallback(callback)
        manager.removePairingCallback(callback)
        assertFalse(callbackCalled)
    }

    @Test
    fun `hidePairingNotification does not crash`() {
        val manager = createManager()
        manager.hidePairingNotification()
    }

    @Test
    fun `pairStatus changes when handler state changes`() {
        val manager = createManager(PairingHandler.PairState.NotPaired)
        assertEquals(PairingHandler.PairState.NotPaired, manager.pairStatus)
        manager.pairingHandler.state = PairingHandler.PairState.Paired
        assertEquals(PairingHandler.PairState.Paired, manager.pairStatus)
    }
}
