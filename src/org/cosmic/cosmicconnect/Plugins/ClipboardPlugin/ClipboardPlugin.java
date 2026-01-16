/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.ClipboardPlugin;


import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.cosmic.cosmicconnect.Core.NetworkPacket;
import org.cosmic.cosmicconnect.Plugins.Plugin;
import org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPacketsFFI;
import static org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPacketsFFIKt.*;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;
import org.cosmic.cosmicconnect.R;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

@PluginFactory.LoadablePlugin
public class ClipboardPlugin extends Plugin {

    /**
     * Packet containing just clipboard contents, sent when a device updates its clipboard.
     * <p>
     * The body should look like so:
     * {
     * "content": "password"
     * }
     */
    private final static String PACKET_TYPE_CLIPBOARD = "kdeconnect.clipboard";

    /**
     * Packet containing clipboard contents and a timestamp that the contents were last updated, sent
     * on first connection
     * <p>
     * The timestamp is milliseconds since epoch. It can be 0, which indicates that the clipboard
     * update time is currently unknown.
     * <p>
     * The body should look like so:
     * {
     * "timestamp": 542904563213,
     * "content": "password"
     * }
     */
    private final static String PACKET_TYPE_CLIPBOARD_CONNECT = "kdeconnect.clipboard.connect";

    @Override
    public @NonNull String getDisplayName() {
        return context.getResources().getString(R.string.pref_plugin_clipboard);
    }

    @Override
    public @NonNull String getDescription() {
        return context.getResources().getString(R.string.pref_plugin_clipboard_desc);
    }

    @Override
    public boolean onPacketReceived(@NonNull org.cosmic.cosmicconnect.NetworkPacket legacyNp) {
        // Convert legacy packet to immutable for type-safe inspection
        NetworkPacket np = NetworkPacket.fromLegacy(legacyNp);

        // Use FFI extension properties for type checking
        if (getIsClipboardUpdate(np)) {
            String content = getClipboardContent(np);
            if (content != null) {
                ClipboardListener.instance(context).setText(content);
            }
            return true;
        } else if (getIsClipboardConnect(np)) {
            Long timestamp = getClipboardTimestamp(np);
            // If the timestamp is null or 0, it means the timestamp is unknown (so do nothing).
            if (timestamp == null || timestamp == 0 ||
                timestamp < ClipboardListener.instance(context).getUpdateTimestamp()) {
                return false;
            }

            String content = getClipboardContent(np);
            if (content != null) {
                ClipboardListener.instance(context).setText(content);
            }
            return true;
        }

        return false;
    }

    private final ClipboardListener.ClipboardObserver observer = this::propagateClipboard;

    void propagateClipboard(String content) {
        // Create packet using FFI wrapper
        NetworkPacket packet = ClipboardPacketsFFI.INSTANCE.createClipboardUpdate(content);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    private void sendConnectPacket() {
        String content = ClipboardListener.instance(context).getCurrentContent();
        if (content == null) {
            // Send clipboard only if it had been initialized
            return;
        }

        // Create packet using FFI wrapper with timestamp
        long timestamp = ClipboardListener.instance(context).getUpdateTimestamp();
        NetworkPacket packet = ClipboardPacketsFFI.INSTANCE.createClipboardConnect(content, timestamp);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    /**
     * Convert immutable NetworkPacket to legacy NetworkPacket for sending
     */
    private org.cosmic.cosmicconnect.NetworkPacket convertToLegacyPacket(NetworkPacket ffi) {
        org.cosmic.cosmicconnect.NetworkPacket legacy =
            new org.cosmic.cosmicconnect.NetworkPacket(ffi.getType());

        // Copy all body fields
        Map<String, Object> body = ffi.getBody();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            legacy.set(entry.getKey(), entry.getValue());
        }

        return legacy;
    }


    @Override
    public boolean onCreate() {
        ClipboardListener.instance(context).registerObserver(observer);
        sendConnectPacket();
        return true;
    }

    @Override
    public void onDestroy() {
        ClipboardListener.instance(context).removeObserver(observer);
    }

    @Override
    public @NonNull String[] getSupportedPacketTypes() {
        return new String[]{PACKET_TYPE_CLIPBOARD, PACKET_TYPE_CLIPBOARD_CONNECT};
    }

    @Override
    public @NonNull String[] getOutgoingPacketTypes() {
        return new String[]{PACKET_TYPE_CLIPBOARD, PACKET_TYPE_CLIPBOARD_CONNECT};
    }

    @Override
    public @NotNull List<@NotNull PluginUiButton> getUiButtons() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && canAccessLogs()) {
            return List.of(new PluginUiButton(context.getString(R.string.send_clipboard), R.drawable.ic_baseline_content_paste_24, parentActivity -> {
                userInitiatedSendClipboard();
                return Unit.INSTANCE;
            }));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public @NotNull List<@NotNull PluginUiMenuEntry> getUiMenuEntries() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !canAccessLogs()) {
            return List.of(new PluginUiMenuEntry(context.getString(R.string.send_clipboard), parentActivity -> {
                userInitiatedSendClipboard();
                return Unit.INSTANCE;
            }));
        } else {
            return Collections.emptyList();
        }
    }

    private void userInitiatedSendClipboard() {
        if (isDeviceInitialized()) {
            ClipboardManager clipboardManager = ContextCompat.getSystemService(this.context, ClipboardManager.class);
            ClipData.Item item;
            if (clipboardManager.hasPrimaryClip()) {
                item = clipboardManager.getPrimaryClip().getItemAt(0);
                String content = item.coerceToText(this.context).toString();
                this.propagateClipboard(content);
                Toast.makeText(this.context, R.string.pref_plugin_clipboard_sent, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean canAccessLogs() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_DENIED;
    }

}
