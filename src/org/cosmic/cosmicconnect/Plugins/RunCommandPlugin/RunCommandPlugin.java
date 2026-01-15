/*
 * SPDX-FileCopyrightText: 2015 Aleix Pol Gonzalez <aleixpol@kde.org>
 * SPDX-FileCopyrightText: 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin;

import static org.cosmic.cosmicconnect.Plugins.RunCommandPlugin.RunCommandWidgetProviderKt.forceRefreshWidgets;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.cosmic.cosmicconnect.Core.NetworkPacket;
import org.cosmic.cosmicconnect.Plugins.Plugin;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment;
import org.cosmic.cosmicconnect.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

@PluginFactory.LoadablePlugin
public class RunCommandPlugin extends Plugin {

    private final static String PACKET_TYPE_RUNCOMMAND = "cosmicconnect.runcommand";
    private final static String PACKET_TYPE_RUNCOMMAND_REQUEST = "cosmicconnect.runcommand.request";
    public final static String KEY_COMMANDS_PREFERENCE = "commands_preference_";

    private final ArrayList<JSONObject> commandList = new ArrayList<>();
    private final ArrayList<CommandsChangedCallback> callbacks = new ArrayList<>();
    private final ArrayList<CommandEntry> commandItems = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    private boolean canAddCommand;

    public void addCommandsUpdatedCallback(CommandsChangedCallback newCallback) {
        callbacks.add(newCallback);
    }

    public void removeCommandsUpdatedCallback(CommandsChangedCallback theCallback) {
        callbacks.remove(theCallback);
    }

    interface CommandsChangedCallback {
        void update();
    }

    public ArrayList<JSONObject> getCommandList() {
        return commandList;
    }

    public ArrayList<CommandEntry> getCommandItems() {
        return commandItems;
    }

    @Override
    public @NonNull String getDisplayName() {
        return context.getResources().getString(R.string.pref_plugin_runcommand);
    }

    @Override
    public @NonNull String getDescription() {
        return context.getResources().getString(R.string.pref_plugin_runcommand_desc);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Nullable
    @Override
    public PluginSettingsFragment getSettingsFragment(Activity activity) {
        return PluginSettingsFragment.newInstance(getPluginKey(), R.xml.runcommand_preferences);
    }

    @Override
    public @NotNull List<@NotNull PluginUiButton> getUiButtons() {
        return List.of(new PluginUiButton(context.getString(R.string.pref_plugin_runcommand), R.drawable.run_command_plugin_icon_24dp, parentActivity -> {
            Intent intent = new Intent(parentActivity, RunCommandActivity.class);
            intent.putExtra("deviceId", getDevice().getDeviceId());
            parentActivity.startActivity(intent);
            return Unit.INSTANCE;
        }));
    }

    @Override
    public boolean onCreate() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        requestCommandList();
        return true;
    }

    @Override
    public boolean onPacketReceived(@NonNull org.cosmic.cosmicconnect.NetworkPacket np) {

        if (np.has("commandList")) {
            commandList.clear();
            try {
                commandItems.clear();
                JSONObject obj = new JSONObject(np.getString("commandList"));
                for (String s : new IteratorIterable<>(obj.keys())) {
                    JSONObject o = obj.getJSONObject(s);
                    o.put("key", s);
                    commandList.add(o);

                    try {
                        commandItems.add(
                                new CommandEntry(o)
                        );
                    } catch (JSONException e) {
                        Log.e("RunCommand", "Error parsing JSON", e);
                    }
                }

                Collections.sort(commandItems, Comparator.comparing(CommandEntry::getName));

                // Used only by RunCommandControlsProviderService to display controls correctly even when device is not available
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    JSONArray array = new JSONArray();

                    for (JSONObject command : commandList) {
                        array.put(command);
                    }

                    sharedPreferences.edit()
                            .putString(KEY_COMMANDS_PREFERENCE + getDevice().getDeviceId(), array.toString())
                            .apply();
                }

                forceRefreshWidgets(context);

            } catch (JSONException e) {
                Log.e("RunCommand", "Error parsing JSON", e);
            }

            for (CommandsChangedCallback aCallback : callbacks) {
                aCallback.update();
            }

            getDevice().onPluginsChanged();

            canAddCommand = np.getBoolean("canAddCommand", false);

            return true;
        }
        return false;
    }

    @Override
    public @NonNull String[] getSupportedPacketTypes() {
        return new String[]{PACKET_TYPE_RUNCOMMAND};
    }

    @Override
    public @NonNull String[] getOutgoingPacketTypes() {
        return new String[]{PACKET_TYPE_RUNCOMMAND_REQUEST};
    }

    public void runCommand(String cmdKey) {
        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("key", cmdKey);
        NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_RUNCOMMAND_REQUEST, body);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    private void requestCommandList() {
        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("requestCommandList", true);
        NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_RUNCOMMAND_REQUEST, body);

        // Convert and send
        getDevice().sendPacket(convertToLegacyPacket(packet));
    }

    public boolean canAddCommand() {
        return canAddCommand;
    }

    void sendSetupPacket() {
        // Create immutable packet
        Map<String, Object> body = new HashMap<>();
        body.put("setup", true);
        NetworkPacket packet = NetworkPacket.create(RunCommandPlugin.PACKET_TYPE_RUNCOMMAND_REQUEST, body);

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

}
