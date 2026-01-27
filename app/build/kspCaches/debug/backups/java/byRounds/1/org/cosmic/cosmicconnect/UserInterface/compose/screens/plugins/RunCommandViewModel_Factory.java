package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;

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
public final class RunCommandViewModel_Factory implements Factory<RunCommandViewModel> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public RunCommandViewModel_Factory(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  @Override
  public RunCommandViewModel get() {
    return newInstance(deviceRegistryProvider.get());
  }

  public static RunCommandViewModel_Factory create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new RunCommandViewModel_Factory(deviceRegistryProvider);
  }

  public static RunCommandViewModel newInstance(DeviceRegistry deviceRegistry) {
    return new RunCommandViewModel(deviceRegistry);
  }
}
