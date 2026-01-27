package org.cosmic.cosmicconnect.UserInterface;

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
public final class PluginSettingsActivity_MembersInjector implements MembersInjector<PluginSettingsActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public PluginSettingsActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<PluginSettingsActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new PluginSettingsActivity_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(PluginSettingsActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity.deviceRegistry")
  public static void injectDeviceRegistry(PluginSettingsActivity instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
