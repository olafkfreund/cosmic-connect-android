package org.cosmic.cosmicconnect.Helpers.SecurityHelpers;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SslHelper_Factory implements Factory<SslHelper> {
  private final Provider<Context> contextProvider;

  private final Provider<RsaHelper> rsaHelperProvider;

  public SslHelper_Factory(Provider<Context> contextProvider,
      Provider<RsaHelper> rsaHelperProvider) {
    this.contextProvider = contextProvider;
    this.rsaHelperProvider = rsaHelperProvider;
  }

  @Override
  public SslHelper get() {
    return newInstance(contextProvider.get(), rsaHelperProvider.get());
  }

  public static SslHelper_Factory create(Provider<Context> contextProvider,
      Provider<RsaHelper> rsaHelperProvider) {
    return new SslHelper_Factory(contextProvider, rsaHelperProvider);
  }

  public static SslHelper newInstance(Context context, RsaHelper rsaHelper) {
    return new SslHelper(context, rsaHelper);
  }
}
