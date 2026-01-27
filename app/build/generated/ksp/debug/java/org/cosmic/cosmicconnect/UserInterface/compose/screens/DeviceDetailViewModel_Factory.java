package org.cosmic.cosmicconnect.UserInterface.compose.screens;

import androidx.lifecycle.SavedStateHandle;
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
public final class DeviceDetailViewModel_Factory implements Factory<DeviceDetailViewModel> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public DeviceDetailViewModel_Factory(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public DeviceDetailViewModel get() {
    return newInstance(deviceRegistryProvider.get(), savedStateHandleProvider.get());
  }

  public static DeviceDetailViewModel_Factory create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new DeviceDetailViewModel_Factory(deviceRegistryProvider, savedStateHandleProvider);
  }

  public static DeviceDetailViewModel newInstance(DeviceRegistry deviceRegistry,
      SavedStateHandle savedStateHandle) {
    return new DeviceDetailViewModel(deviceRegistry, savedStateHandle);
  }
}
