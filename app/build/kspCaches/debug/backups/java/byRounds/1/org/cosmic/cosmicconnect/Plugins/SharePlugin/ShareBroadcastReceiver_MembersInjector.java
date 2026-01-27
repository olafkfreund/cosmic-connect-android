package org.cosmic.cosmicconnect.Plugins.SharePlugin;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class ShareBroadcastReceiver_MembersInjector implements MembersInjector<ShareBroadcastReceiver> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public ShareBroadcastReceiver_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<ShareBroadcastReceiver> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new ShareBroadcastReceiver_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(ShareBroadcastReceiver instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareBroadcastReceiver.deviceRegistry")
  public static void injectDeviceRegistry(ShareBroadcastReceiver instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
