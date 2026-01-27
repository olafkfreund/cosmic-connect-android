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
public final class MprisViewModel_Factory implements Factory<MprisViewModel> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public MprisViewModel_Factory(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  @Override
  public MprisViewModel get() {
    return newInstance(deviceRegistryProvider.get());
  }

  public static MprisViewModel_Factory create(Provider<DeviceRegistry> deviceRegistryProvider) {
    return new MprisViewModel_Factory(deviceRegistryProvider);
  }

  public static MprisViewModel newInstance(DeviceRegistry deviceRegistry) {
    return new MprisViewModel(deviceRegistry);
  }
}
