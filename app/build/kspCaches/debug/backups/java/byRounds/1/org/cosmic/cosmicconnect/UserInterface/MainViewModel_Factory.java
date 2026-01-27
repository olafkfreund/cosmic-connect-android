package org.cosmic.cosmicconnect.UserInterface;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;

@ScopeMetadata
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceRegistry> deviceRegistryProvider;

  private final Provider<DeviceHelper> deviceHelperProvider;

  public MainViewModel_Factory(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    this.contextProvider = contextProvider;
    this.deviceRegistryProvider = deviceRegistryProvider;
    this.deviceHelperProvider = deviceHelperProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(contextProvider.get(), deviceRegistryProvider.get(), deviceHelperProvider.get());
  }

  public static MainViewModel_Factory create(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider,
      Provider<DeviceHelper> deviceHelperProvider) {
    return new MainViewModel_Factory(contextProvider, deviceRegistryProvider, deviceHelperProvider);
  }

  public static MainViewModel newInstance(Context context, DeviceRegistry deviceRegistry,
      DeviceHelper deviceHelper) {
    return new MainViewModel(context, deviceRegistry, deviceHelper);
  }
}
