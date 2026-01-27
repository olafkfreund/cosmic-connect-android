package org.cosmic.cosmicconnect.UserInterface.compose.screens.config;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CustomDevicesViewModel_Factory implements Factory<CustomDevicesViewModel> {
  private final Provider<Context> contextProvider;

  public CustomDevicesViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CustomDevicesViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static CustomDevicesViewModel_Factory create(Provider<Context> contextProvider) {
    return new CustomDevicesViewModel_Factory(contextProvider);
  }

  public static CustomDevicesViewModel newInstance(Context context) {
    return new CustomDevicesViewModel(context);
  }
}
