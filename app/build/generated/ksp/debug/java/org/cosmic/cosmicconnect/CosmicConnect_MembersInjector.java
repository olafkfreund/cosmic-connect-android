package org.cosmic.cosmicconnect;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.RsaHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;

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
public final class CosmicConnect_MembersInjector implements MembersInjector<CosmicConnect> {
  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  private final Provider<RsaHelper> rsaHelperProvider;

  private final Provider<SslHelper> sslHelperProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  public CosmicConnect_MembersInjector(Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<DeviceHelper> deviceHelperProvider, Provider<RsaHelper> rsaHelperProvider,
      Provider<SslHelper> sslHelperProvider, Provider<PluginFactory> pluginFactoryProvider) {
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.deviceHelperProvider = deviceHelperProvider;
    this.rsaHelperProvider = rsaHelperProvider;
    this.sslHelperProvider = sslHelperProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
  }

  public static MembersInjector<CosmicConnect> create(
      Provider<DeviceRegistry> deviceRegistryProvider, Provider<DeviceHelper> deviceHelperProvider,
      Provider<RsaHelper> rsaHelperProvider, Provider<SslHelper> sslHelperProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    return new CosmicConnect_MembersInjector(deviceRegistryProvider, deviceHelperProvider, rsaHelperProvider, sslHelperProvider, pluginFactoryProvider);
  }

  @Override
  public void injectMembers(CosmicConnect instance) {
    injectDeviceRegistry(instance, deviceRegistryProvider.get());
    injectDeviceHelper(instance, deviceHelperProvider.get());
    injectRsaHelper(instance, rsaHelperProvider.get());
    injectSslHelper(instance, sslHelperProvider.get());
    injectPluginFactory(instance, pluginFactoryProvider.get());
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.CosmicConnect.deviceRegistry")
  public static void injectDeviceRegistry(CosmicConnect instance, DeviceRegistry deviceRegistry) {
    instance.deviceRegistry = deviceRegistry;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.CosmicConnect.deviceHelper")
  public static void injectDeviceHelper(CosmicConnect instance, DeviceHelper deviceHelper) {
    instance.deviceHelper = deviceHelper;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.CosmicConnect.rsaHelper")
  public static void injectRsaHelper(CosmicConnect instance, RsaHelper rsaHelper) {
    instance.rsaHelper = rsaHelper;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.CosmicConnect.sslHelper")
  public static void injectSslHelper(CosmicConnect instance, SslHelper sslHelper) {
    instance.sslHelper = sslHelper;
  }

  @InjectedFieldSignature("org.cosmic.cosmicconnect.CosmicConnect.pluginFactory")
  public static void injectPluginFactory(CosmicConnect instance, PluginFactory pluginFactory) {
    instance.pluginFactory = pluginFactory;
  }
}
