package org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin;

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
public final class FindMyPhoneReceiver_MembersInjector implements MembersInjector<FindMyPhoneReceiver> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public FindMyPhoneReceiver_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<FindMyPhoneReceiver> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new FindMyPhoneReceiver_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(FindMyPhoneReceiver instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneReceiver.deviceRegistry")
  public static void injectDeviceRegistry(FindMyPhoneReceiver instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
