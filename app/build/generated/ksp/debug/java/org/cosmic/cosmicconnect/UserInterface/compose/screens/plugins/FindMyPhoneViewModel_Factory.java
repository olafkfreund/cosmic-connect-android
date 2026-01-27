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
public final class FindMyPhoneViewModel_Factory implements Factory<FindMyPhoneViewModel> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public FindMyPhoneViewModel_Factory(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  @Override
  public FindMyPhoneViewModel get() {
    return newInstance(deviceRegistryProvider.get());
  }

  public static FindMyPhoneViewModel_Factory create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new FindMyPhoneViewModel_Factory(deviceRegistryProvider);
  }

  public static FindMyPhoneViewModel newInstance(DeviceRegistry deviceRegistry) {
    return new FindMyPhoneViewModel(deviceRegistry);
  }
}
