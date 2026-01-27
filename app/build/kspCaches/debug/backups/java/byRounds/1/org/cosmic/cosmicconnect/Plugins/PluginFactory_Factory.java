package org.cosmic.cosmicconnect.Plugins;

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
public final class PluginFactory_Factory implements Factory<PluginFactory> {
  private final Provider<Context> contextProvider;

  public PluginFactory_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PluginFactory get() {
    return newInstance(contextProvider.get());
  }

  public static PluginFactory_Factory create(Provider<Context> contextProvider) {
    return new PluginFactory_Factory(contextProvider);
  }

  public static PluginFactory newInstance(Context context) {
    return new PluginFactory(context);
  }
}
