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
public final class PluginSettingsListFragment_MembersInjector implements MembersInjector<PluginSettingsListFragment> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public PluginSettingsListFragment_MembersInjector(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<PluginSettingsListFragment> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new PluginSettingsListFragment_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(PluginSettingsListFragment instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.PluginSettingsListFragment.deviceRegistry")
  public static void injectDeviceRegistry(PluginSettingsListFragment instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
