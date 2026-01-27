package org.cosmic.cosmicconnect;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Backends.BluetoothBackend.BluetoothLinkProvider;
import org.cosmic.cosmicconnect.Backends.LanBackend.LanLinkProvider;
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
public final class BackgroundService_MembersInjector implements MembersInjector<BackgroundService> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<LanLinkProvider> lanLinkProvider;

  private final Provider<BluetoothLinkProvider> bluetoothLinkProvider;

  public BackgroundService_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<LanLinkProvider> lanLinkProvider,
      Provider<BluetoothLinkProvider> bluetoothLinkProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.lanLinkProvider = lanLinkProvider;
    this.bluetoothLinkProvider = bluetoothLinkProvider;
  }

  public static MembersInjector<BackgroundService> create(
      Provider<DeviceRegistry> deviceRegistryProvider, Provider<LanLinkProvider> lanLinkProvider,
      Provider<BluetoothLinkProvider> bluetoothLinkProvider) {
    return new BackgroundService_MembersInjector(deviceRegistryProvider, lanLinkProvider, bluetoothLinkProvider);
  }

  @Override
  public void injectMembers(BackgroundService instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
    injectLanLinkProvider(instance, lanLinkProvider.get());
    injectBluetoothLinkProvider(instance, bluetoothLinkProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.BackgroundService.deviceRegistry")
  public static void injectDeviceRegistry(BackgroundService instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.BackgroundService.lanLinkProvider")
  public static void injectLanLinkProvider(BackgroundService instance,
      LanLinkProvider lanLinkProvider) {
    instance.lanLinkProvider = lanLinkProvider;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.BackgroundService.bluetoothLinkProvider")
  public static void injectBluetoothLinkProvider(BackgroundService instance,
      BluetoothLinkProvider bluetoothLinkProvider) {
    instance.bluetoothLinkProvider = bluetoothLinkProvider;
  }
}
