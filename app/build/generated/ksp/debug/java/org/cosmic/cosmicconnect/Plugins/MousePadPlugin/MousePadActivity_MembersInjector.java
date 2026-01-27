package org.cosmic.cosmicconnect.Plugins.MousePadPlugin;

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
public final class MousePadActivity_MembersInjector implements MembersInjector<MousePadActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public MousePadActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<MousePadActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new MousePadActivity_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(MousePadActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadActivity.deviceRegistry")
  public static void injectDeviceRegistry(MousePadActivity instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
