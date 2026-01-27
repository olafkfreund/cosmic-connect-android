package org.cosmic.cosmicconnect.UserInterface.compose.screens.config;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;

@ScopeMetadata
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
public final class PluginSettingsViewModel_Factory implements Factory<PluginSettingsViewModel> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  public PluginSettingsViewModel_Factory(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
  }

  @Override
  public PluginSettingsViewModel get() {
    return newInstance(deviceRegistryProvider.get(), pluginFactoryProvider.get());
  }

  public static PluginSettingsViewModel_Factory create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    return new PluginSettingsViewModel_Factory(deviceRegistryProvider, pluginFactoryProvider);
  }

  public static PluginSettingsViewModel newInstance(DeviceRegistry deviceRegistry,
      PluginFactory pluginFactory) {
    return new PluginSettingsViewModel(deviceRegistry, pluginFactory);
  }
}
