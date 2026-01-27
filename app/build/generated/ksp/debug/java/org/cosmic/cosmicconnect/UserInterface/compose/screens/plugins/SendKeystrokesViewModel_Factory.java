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
public final class SendKeystrokesViewModel_Factory implements Factory<SendKeystrokesViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceRegistry> deviceRegistryProvider;

  public SendKeystrokesViewModel_Factory(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider) {
    this.contextProvider = contextProvider;
    this.deviceRegistryProvider = deviceRegistryProvider;
  }

  @Override
  public SendKeystrokesViewModel get() {
    return newInstance(contextProvider.get(), deviceRegistryProvider.get());
  }

  public static SendKeystrokesViewModel_Factory create(Provider<Context> contextProvider,
      Provider<DeviceRegistry> deviceRegistryProvider) {
    return new SendKeystrokesViewModel_Factory(contextProvider, deviceRegistryProvider);
  }

  public static SendKeystrokesViewModel newInstance(Context context,
      DeviceRegistry deviceRegistry) {
    return new SendKeystrokesViewModel(context, deviceRegistry);
  }
}
