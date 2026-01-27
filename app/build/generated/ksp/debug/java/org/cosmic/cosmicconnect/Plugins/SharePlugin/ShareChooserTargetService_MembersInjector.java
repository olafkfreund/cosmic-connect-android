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
public final class ShareChooserTargetService_MembersInjector implements MembersInjector<ShareChooserTargetService> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public ShareChooserTargetService_MembersInjector(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  public static MembersInjector<ShareChooserTargetService> create(
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new ShareChooserTargetService_MembersInjector(deviceRegistryProvider);
  }

  @Override
  public void injectMembers(ShareChooserTargetService instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareChooserTargetService.deviceRegistry")
  public static void injectDeviceRegistry(ShareChooserTargetService instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }
}
