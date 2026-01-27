package org.cosmic.cosmicconnect.Backends.BluetoothBackend;

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
public final class BluetoothLinkProvider_Factory implements Factory<BluetoothLinkProvider> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  private final Provider<SslHelper> sslHelperProvider;

  public BluetoothLinkProvider_Factory(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider, Provider<SslHelper> sslHelperProvider) {
    this.contextProvider = contextProvider;
    this.deviceHelperProvider = deviceHelperProvider;
    this.sslHelperProvider = sslHelperProvider;
  }

  @Override
  public BluetoothLinkProvider get() {
    return newInstance(contextProvider.get(), deviceHelperProvider.get(), sslHelperProvider.get());
  }

  public static BluetoothLinkProvider_Factory create(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider, Provider<SslHelper> sslHelperProvider) {
    return new BluetoothLinkProvider_Factory(contextProvider, deviceHelperProvider, sslHelperProvider);
  }

  public static BluetoothLinkProvider newInstance(Context context, DeviceHelper deviceHelper,
      SslHelper sslHelper) {
    return new BluetoothLinkProvider(context, deviceHelper, sslHelper);
  }
}
