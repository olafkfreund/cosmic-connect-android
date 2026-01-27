package org.cosmic.cosmicconnect.Plugins.PresenterPlugin;

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
public final class PresenterActivity_MembersInjector implements MembersInjector<PresenterActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public PresenterActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<PresenterActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new PresenterActivity_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(PresenterActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity.deviceRegistry")
  public static void injectDeviceRegistry(PresenterActivity instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
