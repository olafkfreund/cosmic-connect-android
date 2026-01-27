package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;

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
public final class ShareViewModel_Factory implements Factory<ShareViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public ShareViewModel_Factory(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider) {
    this.contextProvider = contextProvider;
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  @Override
  public ShareViewModel get() {
    return newInstance(contextProvider.get(), deviceRegistryProvider.get());
  }

  public static ShareViewModel_Factory create(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new ShareViewModel_Factory(contextProvider, deviceRegistryProvider);
  }

  public static ShareViewModel newInstance(Context context, DeviceRegistry deviceRegistry) {
    return new ShareViewModel(context, deviceRegistry);
  }
}
