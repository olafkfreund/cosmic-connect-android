package org.cosmic.cosmicconnect.UserInterface;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;

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
public final class PluginSettingsFragment_MembersInjector implements MembersInjector<PluginSettingsFragment> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  public PluginSettingsFragment_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
  }

  public static MembersInjector<PluginSettingsFragment> create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    return new PluginSettingsFragment_MembersInjector(deviceRegistryProvider, pluginFactoryProvider);
  }

  @Override
  public void injectMembers(PluginSettingsFragment instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
    injectPluginFactory(instance, pluginFactoryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment.deviceRegistry")
  public static void injectDeviceRegistry(PluginSettingsFragment instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment.pluginFactory")
  public static void injectPluginFactory(PluginSettingsFragment instance,
      PluginFactory pluginFactory) {
    instance.pluginFactory = pluginFactory;
  }
}
