package org.cosmic.cosmicconnect.Helpers;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
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
public final class DeviceHelper_Factory implements Factory<DeviceHelper> {
  private final Provider<Context> contextProvider;

  private final Provider<SslHelper> sslHelperProvider;

  private final Provider<PluginFactory> pluginFactoryProvider;

  public DeviceHelper_Factory(Provider<Context> contextProvider,
      Provider<SslHelper> sslHelperProvider, Provider<PluginFactory> pluginFactoryProvider) {
    this.contextProvider = contextProvider;
    this.sslHelperProvider = sslHelperProvider;
    this.pluginFactoryProvider = pluginFactoryProvider;
  }

  @Override
  public DeviceHelper get() {
    return newInstance(contextProvider.get(), sslHelperProvider.get(), pluginFactoryProvider.get());
  }

  public static DeviceHelper_Factory create(Provider<Context> contextProvider,
      Provider<SslHelper> sslHelperProvider, Provider<PluginFactory> pluginFactoryProvider) {
    return new DeviceHelper_Factory(contextProvider, sslHelperProvider, pluginFactoryProvider);
  }

  public static DeviceHelper newInstance(Context context, SslHelper sslHelper,
      PluginFactory pluginFactory) {
    return new DeviceHelper(context, sslHelper, pluginFactory);
  }
}
