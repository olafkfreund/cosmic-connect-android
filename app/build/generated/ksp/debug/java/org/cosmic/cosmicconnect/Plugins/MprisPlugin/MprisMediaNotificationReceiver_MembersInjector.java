package org.cosmic.cosmicconnect.Plugins.MprisPlugin;

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
public final class MprisMediaNotificationReceiver_MembersInjector implements MembersInjector<MprisMediaNotificationReceiver> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<MprisMediaSession> mprisMediaSessionProvider;

  public MprisMediaNotificationReceiver_MembersInjector(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<MprisMediaSession> mprisMediaSessionProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.mprisMediaSessionProvider = mprisMediaSessionProvider;
  }

  public static MembersInjector<MprisMediaNotificationReceiver> create(
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<MprisMediaSession> mprisMediaSessionProvider) {
    return new MprisMediaNotificationReceiver_MembersInjector(deviceRegistryProvider, mprisMediaSessionProvider);
  }

  @Override
  public void injectMembers(MprisMediaNotificationReceiver instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
    injectMprisMediaSession(instance, mprisMediaSessionProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaNotificationReceiver.deviceRegistry")
  public static void injectDeviceRegistry(MprisMediaNotificationReceiver instance,
      DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaNotificationReceiver.mprisMediaSession")
  public static void injectMprisMediaSession(MprisMediaNotificationReceiver instance,
      MprisMediaSession mprisMediaSession) {
    instance.mprisMediaSession = mprisMediaSession;
  }
}
