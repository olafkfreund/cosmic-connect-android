package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins;

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
public final class NotificationFilterViewModel_Factory implements Factory<NotificationFilterViewModel> {
  private final Provider<Context> contextProvider;

  public NotificationFilterViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationFilterViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static NotificationFilterViewModel_Factory create(Provider<Context> contextProvider) {
    return new NotificationFilterViewModel_Factory(contextProvider);
  }

  public static NotificationFilterViewModel newInstance(Context context) {
    return new NotificationFilterViewModel(context);
  }
}
