package org.cosmic.cosmicconnect.Plugins.SftpPlugin;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment_MembersInjector;

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
public final class SftpSettingsFragment_MembersInjector implements MembersInjector<SftpSettingsFragment> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  private final Provider<DeviceRegistry> deviceRegistryProvider2;

  public SftpSettingsFragment_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider,
      Provider<DeviceRegistry> deviceRegistryProvider2) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
    this.deviceRegistryProvider2 = deviceRegistryProvider2;
  }

  public static MembersInjector<SftpSettingsFragment> create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider,
      Provider<DeviceRegistry> deviceRegistryProvider2) {
    return new SftpSettingsFragment_MembersInjector(deviceRegistryProvider, pluginFactoryProvider, deviceRegistryProvider2);
  }

  @Override
  public void injectMembers(SftpSettingsFragment instance) {
    PluginSettingsFragment_MembersInjector.injectDeviceRegistry(instance, deviceRegistryProvider.get());
    PluginSettingsFragment_MembersInjector.injectPluginFactory(instance, pluginFactoryProvider.get());
    injectDeviceRegistry(instance, deviceRegistryProvider2.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpSettingsFragment.deviceRegistry")
  public static void injectDeviceRegistry(SftpSettingsFragment instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
