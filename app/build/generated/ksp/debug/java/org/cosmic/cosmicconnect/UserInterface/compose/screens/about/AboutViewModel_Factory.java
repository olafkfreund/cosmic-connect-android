package org.cosmic.cosmicconnect.UserInterface.compose.screens.about;

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
public final class AboutViewModel_Factory implements Factory<AboutViewModel> {
  private final Provider<Context> contextProvider;

  public AboutViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AboutViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static AboutViewModel_Factory create(Provider<Context> contextProvider) {
    return new AboutViewModel_Factory(contextProvider);
  }

  public static AboutViewModel newInstance(Context context) {
    return new AboutViewModel(context);
  }
}
