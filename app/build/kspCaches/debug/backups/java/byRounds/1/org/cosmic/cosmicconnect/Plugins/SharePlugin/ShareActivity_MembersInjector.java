package org.cosmic.cosmicconnect.Plugins.SharePlugin;

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
public final class ShareActivity_MembersInjector implements MembersInjector<ShareActivity> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public ShareActivity_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<ShareActivity> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new ShareActivity_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(ShareActivity instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareActivity.deviceRegistry")
  public static void injectDeviceRegistry(ShareActivity instance, DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
