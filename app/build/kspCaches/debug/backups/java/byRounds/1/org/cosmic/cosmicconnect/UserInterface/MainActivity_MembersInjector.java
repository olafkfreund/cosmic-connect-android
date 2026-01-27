package org.cosmic.cosmicconnect.UserInterface;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  public MainActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.deviceHelperProvider = deviceHelperProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    return new MainActivity_MembersInjector(deviceRegistryProvider, deviceHelperProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
    injectDeviceHelper(instance, deviceHelperProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.MainActivity.deviceRegistry")
  public static void injectDeviceRegistry(MainActivity instance, DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.UserInterface.MainActivity.deviceHelper")
  public static void injectDeviceHelper(MainActivity instance, DeviceHelper deviceHelper) {
    instance.deviceHelper = deviceHelper;
  }
}
