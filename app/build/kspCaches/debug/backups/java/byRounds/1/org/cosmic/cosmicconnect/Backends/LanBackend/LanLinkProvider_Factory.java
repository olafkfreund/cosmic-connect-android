package org.cosmic.cosmicconnect.Backends.LanBackend;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper;

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
public final class LanLinkProvider_Factory implements Factory<LanLinkProvider> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  private final Provider<SslHelper> sslHelperProvider;

  public LanLinkProvider_Factory(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider, Provider<SslHelper> sslHelperProvider) {
    this.contextProvider = contextProvider;
    this.deviceHelperProvider = deviceHelperProvider;
    this.sslHelperProvider = sslHelperProvider;
  }

  @Override
  public LanLinkProvider get() {
    return newInstance(contextProvider.get(), deviceHelperProvider.get(), sslHelperProvider.get());
  }

  public static LanLinkProvider_Factory create(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider, Provider<SslHelper> sslHelperProvider) {
    return new LanLinkProvider_Factory(contextProvider, deviceHelperProvider, sslHelperProvider);
  }

  public static LanLinkProvider newInstance(Context context, DeviceHelper deviceHelper,
      SslHelper sslHelper) {
    return new LanLinkProvider(context, deviceHelper, sslHelper);
  }
}
