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
public final class ComposeSendActivity_MembersInjector implements MembersInjector<ComposeSendActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public ComposeSendActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<ComposeSendActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new ComposeSendActivity_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(ComposeSendActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.MousePadPlugin.ComposeSendActivity.deviceRegistry")
  public static void injectDeviceRegistry(ComposeSendActivity instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
