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
public final class LicensesViewModel_Factory implements Factory<LicensesViewModel> {
  private final Provider<Context> contextProvider;

  public LicensesViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LicensesViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static LicensesViewModel_Factory create(Provider<Context> contextProvider) {
    return new LicensesViewModel_Factory(contextProvider);
  }

  public static LicensesViewModel newInstance(Context context) {
    return new LicensesViewModel(context);
  }
}
