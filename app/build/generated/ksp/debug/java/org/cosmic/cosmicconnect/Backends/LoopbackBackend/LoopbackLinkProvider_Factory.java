package org.cosmic.cosmicconnect.Backends.LoopbackBackend;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;

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
public final class LoopbackLinkProvider_Factory implements Factory<LoopbackLinkProvider> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  public LoopbackLinkProvider_Factory(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    this.contextProvider = contextProvider;
    this.deviceHelperProvider = deviceHelperProvider;
  }

  @Override
  public LoopbackLinkProvider get() {
    return newInstance(contextProvider.get(), deviceHelperProvider.get());
  }

  public static LoopbackLinkProvider_Factory create(Provider<Context> contextProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    return new LoopbackLinkProvider_Factory(contextProvider, deviceHelperProvider);
  }

  public static LoopbackLinkProvider newInstance(Context context, DeviceHelper deviceHelper) {
    return new LoopbackLinkProvider(context, deviceHelper);
  }
}
