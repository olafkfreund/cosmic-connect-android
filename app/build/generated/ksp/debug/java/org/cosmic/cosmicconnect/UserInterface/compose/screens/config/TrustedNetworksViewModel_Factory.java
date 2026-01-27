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
public final class TrustedNetworksViewModel_Factory implements Factory<TrustedNetworksViewModel> {
  private final Provider<Context> contextProvider;

  public TrustedNetworksViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TrustedNetworksViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static TrustedNetworksViewModel_Factory create(Provider<Context> contextProvider) {
    return new TrustedNetworksViewModel_Factory(contextProvider);
  }

  public static TrustedNetworksViewModel newInstance(Context context) {
    return new TrustedNetworksViewModel(context);
  }
}
