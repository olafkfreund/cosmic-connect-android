/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.MprisReceiverPlugin;

import android.content.ComponentName;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.apache.commons.lang3.StringUtils;
import org.cosmic.cosmicconnect.Helpers.AppsHelper;
import org.cosmic.cosmicconnect.Helpers.ThreadHelper;
import org.cosmic.cosmicconnect.NetworkPacket;
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationReceiver;
import org.cosmic.cosmicconnect.Plugins.Plugin;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;
import org.cosmic.cosmicconnect.UserInterface.MainActivity;
import org.cosmic.cosmicconnect.UserInterface.StartActivityAlertDialogFragment;
import org.cosmic.cosmicconnect.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PluginFactory.LoadablePlugin
public class MprisReceiverPlugin extends Plugin {
    private final static String PACKET_TYPE_MPRIS = "cosmicconnect.mpris";
    private final static String PACKET_TYPE_MPRIS_REQUEST = "cosmicconnect.mpris.request";

    private static final String TAG = "MprisReceiver";

    // TODO: Those two are always accessed together, merge them
    private HashMap<String, MprisReceiverPlayer> players;
    private HashMap<String, MprisReceiverCallback> playerCbs;

    private MediaSessionChangeListener mediaSessionChangeListener;

    public @NonNull String getDeviceId() {
        return device.getDeviceId();
    }

    @Override
    public boolean onCreate() {

        if (!hasPermission())
            return false;
        players = new HashMap<>();
        playerCbs = new HashMap<>();
        try {
            MediaSessionManager manager = ContextCompat.getSystemService(context, MediaSessionManager.class);
            if (null == manager)
                return false;

            assert(mediaSessionChangeListener == null);
            mediaSessionChangeListener = new MediaSessionChangeListener();
            manager.addOnActiveSessionsChangedListener(mediaSessionChangeListener, new ComponentName(context, NotificationReceiver.class), new Handler(Looper.getMainLooper()));

            createPlayers(manager.getActiveSessions(new ComponentName(context, NotificationReceiver.class)));
            sendPlayerList();
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaSessionManager manager = ContextCompat.getSystemService(context, MediaSessionManager.class);
        if (manager != null && mediaSessionChangeListener != null) {
            manager.removeOnActiveSessionsChangedListener(mediaSessionChangeListener);
            mediaSessionChangeListener = null;
        }
    }

    private void createPlayers(List<MediaController> sessions) {
        for (MediaController controller : sessions) {
            createPlayer(controller);
        }
    }

    @Override
    public @NonNull String getDisplayName() {
        return context.getResources().getString(R.string.pref_plugin_mprisreceiver);
    }

    @Override
    public @NonNull String getDescription() {
        return context.getResources().getString(R.string.pref_plugin_mprisreceiver_desc);
    }

    @Override
    public boolean onPacketReceived(@NonNull org.cosmic.cosmicconnect.NetworkPacket np) {
        if (np.getBoolean("requestPlayerList")) {
            sendPlayerList();
            return true;
        }

        if (!np.has("player")) {
            return false;
        }
        MprisReceiverPlayer player = players.get(np.getString("player"));

        if (null == player) {
            return false;
        }
        String artUrl = np.getString("albumArtUrl", "");
        if (!artUrl.isEmpty()) {
            String playerName = player.getName();
            MprisReceiverCallback cb = playerCbs.get(playerName);
            if (cb == null) {
                Log.e(TAG, "no callback for " + playerName + " (player likely stopped)");
                return false;
            }
            // run it on a different thread to avoid blocking
            ThreadHelper.execute(() -> sendAlbumArt(playerName, cb, artUrl));
            return true;
        }

        if (np.getBoolean("requestNowPlaying", false)) {
            sendMetadata(player);
            return true;
        }

        if (np.has("SetPosition")) {
            long position = np.getLong("SetPosition", 0);
            player.setPosition(position);
        }

        if (np.has("setVolume")) {
            int volume = np.getInt("setVolume", 100);
            player.setVolume(volume);
            //Setting volume doesn't seem to always trigger the callback
            sendMetadata(player);
        }

        if (np.has("action")) {
            String action = np.getString("action");

            switch (action) {
                case "Play":
                    player.play();
                    break;
                case "Pause":
                    player.pause();
                    break;
                case "PlayPause":
                    player.playPause();
                    break;
                case "Next":
                    player.next();
                    break;
                case "Previous":
                    player.previous();
                    break;
                case "Stop":
                    player.stop();
                    break;
            }
        }

        return true;
    }

    @Override
    public @NonNull String[] getSupportedPacketTypes() {
        return new String[]{PACKET_TYPE_MPRIS_REQUEST};
    }

    @Override
    public @NonNull String[] getOutgoingPacketTypes() {
        return new String[]{PACKET_TYPE_MPRIS};
    }

    private final class MediaSessionChangeListener implements MediaSessionManager.OnActiveSessionsChangedListener {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {

            if (null == controllers) {
                return;
            }

            // Make a copy to avoid ConcurrentModificationException
            ArrayList<MprisReceiverPlayer> playersCopy = new ArrayList<>(players.values());
            for (MprisReceiverPlayer p : playersCopy) {
                p.getController().unregisterCallback(Objects.requireNonNull(playerCbs.get(p.getName())));
            }
            playerCbs.clear();
            players.clear();

            createPlayers(controllers);
            sendPlayerList();

        }
    }

    private void createPlayer(MediaController controller) {
        // Skip the media session we created ourselves as COSMIC Connect
        if (controller.getPackageName().equals(context.getPackageName())) return;

        MprisReceiverPlayer player = new MprisReceiverPlayer(controller, AppsHelper.appNameLookup(context, controller.getPackageName()));
        MprisReceiverCallback cb = new MprisReceiverCallback(this, player);
        controller.registerCallback(cb, new Handler(Looper.getMainLooper()));
        playerCbs.put(player.getName(), cb);
        players.put(player.getName(), player);
    }

    private void sendPlayerList() {
        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("playerList", new ArrayList<>(players.keySet()));
        body.put("supportAlbumArtPayload", true);
        NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_MPRIS, body);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    void sendAlbumArt(String playerName, @NonNull MprisReceiverCallback cb, @Nullable String requestedUrl) {
        // NOTE: It is possible that the player gets killed in the middle of this method.
        // The proper thing to do this case would be to abort the send - but that gets into the
        //   territory of async cancellation or putting a lock.
        // For now, we just continue to send the art- cb stores the bitmap, so it will be valid.
        //   cb will get GC'd after this method completes.
        String localArtUrl = cb.getArtUrl();
        if (localArtUrl == null) {
            Log.w(TAG, "art not found!");
            return;
        }
        String artUrl = requestedUrl == null ? localArtUrl : requestedUrl;
        if (requestedUrl != null && !requestedUrl.contentEquals(localArtUrl)) {
            Log.w(TAG, "sendAlbumArt: Doesn't match current url");
            Log.d(TAG, "current:   " + localArtUrl);
            Log.d(TAG, "requested: " + requestedUrl);
            return;
        }
        byte[] p = cb.getArtAsArray();
        if (p == null) {
            Log.w(TAG, "sendAlbumArt: Failed to get art stream");
            return;
        }

        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("player", playerName);
        body.put("transferringAlbumArt", true);
        body.put("albumArtUrl", artUrl);
        NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_MPRIS, body);

        // Convert to legacy and set payload
        org.cosmic.cosmicconnect.NetworkPacket np = convertToLegacyPacket(packet);
        np.setPayload(new org.cosmic.cosmicconnect.NetworkPacket.Payload(p));

        // Send
        getDevice().sendPacket(np);
    }

    void sendMetadata(MprisReceiverPlayer player) {
        // Prepare all data
        String nowPlaying = Stream.of(player.getArtist(), player.getTitle())
            .filter(StringUtils::isNotEmpty).collect(Collectors.joining(" - "));
        String artUrl = "";
        MprisReceiverCallback cb = playerCbs.get(player.getName());
        if (cb != null) {
            String url = cb.getArtUrl();
            if (url != null) {
                artUrl = url;
            }
        }

        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("player", player.getName());
        body.put("title", player.getTitle());
        body.put("artist", player.getArtist());
        body.put("nowPlaying", nowPlaying); // GSConnect 50 (so, Ubuntu 22.04) needs this
        body.put("album", player.getAlbum());
        body.put("isPlaying", player.isPlaying());
        body.put("pos", player.getPosition());
        body.put("length", player.getLength());
        body.put("canPlay", player.canPlay());
        body.put("canPause", player.canPause());
        body.put("canGoPrevious", player.canGoPrevious());
        body.put("canGoNext", player.canGoNext());
        body.put("canSeek", player.canSeek());
        body.put("volume", player.getVolume());
        body.put("albumArtUrl", artUrl);
        NetworkPacket packet = NetworkPacket.create(MprisReceiverPlugin.PACKET_TYPE_MPRIS, body);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    @Override
    public boolean checkRequiredPermissions() {
        //Notifications use a different kind of permission, because it was added before the current runtime permissions model
        return hasPermission();
    }

    @Override
    public @NonNull DialogFragment getPermissionExplanationDialog() {
        return new StartActivityAlertDialogFragment.Builder()
                .setTitle(R.string.pref_plugin_mpris)
                .setMessage(R.string.no_permission_mprisreceiver)
                .setPositiveButton(R.string.open_settings)
                .setNegativeButton(R.string.cancel)
                .setIntentAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .setStartForResult(true)
                .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                .create();
    }

    private boolean hasPermission() {
        String notificationListenerList = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return notificationListenerList != null && notificationListenerList.contains(context.getPackageName());
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

}
