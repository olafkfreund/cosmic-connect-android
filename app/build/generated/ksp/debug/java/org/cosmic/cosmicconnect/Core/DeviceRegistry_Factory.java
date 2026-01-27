package org.cosmic.cosmicconnect.Core;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DeviceRegistry_Factory implements Factory<DeviceRegistry> {
  private final Provider<Context> contextProvider;

  private final Provider<SslHelper> sslHelperProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  public DeviceRegistry_Factory(Provider<Context> contextProvider,
      Provider<SslHelper> sslHelperProvider, Provider<DeviceHelper> deviceHelperProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    this.contextProvider = contextProvider;
    this.sslHelperProvider = sslHelperProvider;
    this.deviceHelperProvider = deviceHelperProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
  }

  @Override
  public DeviceRegistry get() {
    return newInstance(contextProvider.get(), sslHelperProvider.get(), deviceHelperProvider.get(), pluginFactoryProvider.get());
  }

  public static DeviceRegistry_Factory create(Provider<Context> contextProvider,
      Provider<SslHelper> sslHelperProvider, Provider<DeviceHelper> deviceHelperProvider,
      Provider<PluginFactory> pluginFactoryProvider) {
    return new DeviceRegistry_Factory(contextProvider, sslHelperProvider, deviceHelperProvider, pluginFactoryProvider);
  }

  public static DeviceRegistry newInstance(Context context, SslHelper sslHelper,
      DeviceHelper deviceHelper, PluginFactory pluginFactory) {
    return new DeviceRegistry(context, sslHelper, deviceHelper, pluginFactory);
  }
}
