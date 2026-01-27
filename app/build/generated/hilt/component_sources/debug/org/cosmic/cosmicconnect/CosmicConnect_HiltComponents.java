package org.cosmic.cosmicconnect;

import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.components.ViewComponent;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.components.ViewWithFragmentComponent;
import dagger.hilt.android.flags.FragmentGetContextFix;
import dagger.hilt.android.flags.HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories_ActivityModule;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ViewModelModule;
import dagger.hilt.android.internal.managers.ActivityComponentManager;
import dagger.hilt.android.internal.managers.FragmentComponentManager;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_LifecycleModule;
import dagger.hilt.android.internal.managers.HiltWrapper_SavedStateHandleModule;
import dagger.hilt.android.internal.managers.ServiceComponentManager;
import dagger.hilt.android.internal.managers.ViewComponentManager;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.HiltWrapper_ActivityModule;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ServiceScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.android.scopes.ViewScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.migration.DisableInstallInCheck;
import javax.annotation.processing.Generated;
import javax.inject.Singleton;
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.DigitizerActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneReceiver_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.ComposeSendActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.SendKeystrokesToHostActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaNotificationReceiver_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationFilterActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.RunCommandPlugin.RunCommandActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpSettingsFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.SendFileActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareBroadcastReceiver_GeneratedInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareChooserTargetService_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.About.AboutFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.About.LicensesActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.CustomDevicesActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.DeviceFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.MainActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.MainViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.PairingFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsListFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.SettingsFragment_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.TrustedNetworksActivity_GeneratedInjector;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel_HiltModules;
import org.cosmic.cosmicconnect.di.AppModule;
import org.cosmic.cosmicconnect.di.HiltBridges;

@Generated("dagger.hilt.processor.internal.root.RootProcessor")
public final class CosmicConnect_HiltComponents {
  private CosmicConnect_HiltComponents() {
  }

  @Module(
      subcomponents = ServiceC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ServiceCBuilderModule {
    @Binds
    ServiceComponentBuilder bind(ServiceC.Builder builder);
  }

  @Module(
      subcomponents = ActivityRetainedC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityRetainedCBuilderModule {
    @Binds
    ActivityRetainedComponentBuilder bind(ActivityRetainedC.Builder builder);
  }

  @Module(
      subcomponents = ActivityC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityCBuilderModule {
    @Binds
    ActivityComponentBuilder bind(ActivityC.Builder builder);
  }

  @Module(
      subcomponents = ViewModelC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewModelCBuilderModule {
    @Binds
    ViewModelComponentBuilder bind(ViewModelC.Builder builder);
  }

  @Module(
      subcomponents = ViewC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewCBuilderModule {
    @Binds
    ViewComponentBuilder bind(ViewC.Builder builder);
  }

  @Module(
      subcomponents = FragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface FragmentCBuilderModule {
    @Binds
    FragmentComponentBuilder bind(FragmentC.Builder builder);
  }

  @Module(
      subcomponents = ViewWithFragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewWithFragmentCBuilderModule {
    @Binds
    ViewWithFragmentComponentBuilder bind(ViewWithFragmentC.Builder builder);
  }

  @Component(
      modules = {
          AppModule.class,
          ApplicationContextModule.class,
          ActivityRetainedCBuilderModule.class,
          ServiceCBuilderModule.class,
          HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule.class
      }
  )
  @Singleton
  public abstract static class SingletonC implements FragmentGetContextFix.FragmentGetContextFixEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
      ServiceComponentManager.ServiceComponentBuilderEntryPoint,
      SingletonComponent,
      GeneratedComponent,
      CosmicConnect_GeneratedInjector,
      FindMyPhoneReceiver_GeneratedInjector,
      MprisMediaNotificationReceiver_GeneratedInjector,
      ShareBroadcastReceiver_GeneratedInjector,
      HiltBridges {
  }

  @Subcomponent
  @ServiceScoped
  public abstract static class ServiceC implements ServiceComponent,
      GeneratedComponent,
      BackgroundService_GeneratedInjector,
      ShareChooserTargetService_GeneratedInjector {
    @Subcomponent.Builder
    abstract interface Builder extends ServiceComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AboutViewModel_HiltModules.KeyModule.class,
          ActivityCBuilderModule.class,
          ViewModelCBuilderModule.class,
          CustomDevicesViewModel_HiltModules.KeyModule.class,
          DeviceDetailViewModel_HiltModules.KeyModule.class,
          DeviceListViewModel_HiltModules.KeyModule.class,
          DigitizerViewModel_HiltModules.KeyModule.class,
          FindMyPhoneViewModel_HiltModules.KeyModule.class,
          HiltWrapper_ActivityRetainedComponentManager_LifecycleModule.class,
          HiltWrapper_SavedStateHandleModule.class,
          LicensesViewModel_HiltModules.KeyModule.class,
          MainViewModel_HiltModules.KeyModule.class,
          MousePadViewModel_HiltModules.KeyModule.class,
          MprisViewModel_HiltModules.KeyModule.class,
          NotificationFilterViewModel_HiltModules.KeyModule.class,
          PluginSettingsViewModel_HiltModules.KeyModule.class,
          RunCommandViewModel_HiltModules.KeyModule.class,
          SendKeystrokesViewModel_HiltModules.KeyModule.class,
          SettingsViewModel_HiltModules.KeyModule.class,
          ShareViewModel_HiltModules.KeyModule.class,
          TrustedNetworksViewModel_HiltModules.KeyModule.class
      }
  )
  @ActivityRetainedScoped
  public abstract static class ActivityRetainedC implements ActivityRetainedComponent,
      ActivityComponentManager.ActivityComponentBuilderEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityRetainedComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          FragmentCBuilderModule.class,
          ViewCBuilderModule.class,
          HiltWrapper_ActivityModule.class,
          HiltWrapper_DefaultViewModelFactories_ActivityModule.class
      }
  )
  @ActivityScoped
  public abstract static class ActivityC implements ActivityComponent,
      DefaultViewModelFactories.ActivityEntryPoint,
      HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint,
      FragmentComponentManager.FragmentComponentBuilderEntryPoint,
      ViewComponentManager.ViewComponentBuilderEntryPoint,
      GeneratedComponent,
      DigitizerActivity_GeneratedInjector,
      FindMyPhoneActivity_GeneratedInjector,
      ComposeSendActivity_GeneratedInjector,
      MousePadActivity_GeneratedInjector,
      SendKeystrokesToHostActivity_GeneratedInjector,
      MprisActivity_GeneratedInjector,
      NotificationFilterActivity_GeneratedInjector,
      PresenterActivity_GeneratedInjector,
      RunCommandActivity_GeneratedInjector,
      SendFileActivity_GeneratedInjector,
      ShareActivity_GeneratedInjector,
      LicensesActivity_GeneratedInjector,
      CustomDevicesActivity_GeneratedInjector,
      MainActivity_GeneratedInjector,
      PluginSettingsActivity_GeneratedInjector,
      TrustedNetworksActivity_GeneratedInjector {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AboutViewModel_HiltModules.BindsModule.class,
          CustomDevicesViewModel_HiltModules.BindsModule.class,
          DeviceDetailViewModel_HiltModules.BindsModule.class,
          DeviceListViewModel_HiltModules.BindsModule.class,
          DigitizerViewModel_HiltModules.BindsModule.class,
          FindMyPhoneViewModel_HiltModules.BindsModule.class,
          HiltWrapper_HiltViewModelFactory_ViewModelModule.class,
          LicensesViewModel_HiltModules.BindsModule.class,
          MainViewModel_HiltModules.BindsModule.class,
          MousePadViewModel_HiltModules.BindsModule.class,
          MprisViewModel_HiltModules.BindsModule.class,
          NotificationFilterViewModel_HiltModules.BindsModule.class,
          PluginSettingsViewModel_HiltModules.BindsModule.class,
          RunCommandViewModel_HiltModules.BindsModule.class,
          SendKeystrokesViewModel_HiltModules.BindsModule.class,
          SettingsViewModel_HiltModules.BindsModule.class,
          ShareViewModel_HiltModules.BindsModule.class,
          TrustedNetworksViewModel_HiltModules.BindsModule.class
      }
  )
  @ViewModelScoped
  public abstract static class ViewModelC implements ViewModelComponent,
      HiltViewModelFactory.ViewModelFactoriesEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewModelComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewC implements ViewComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewComponentBuilder {
    }
  }

  @Subcomponent(
      modules = ViewWithFragmentCBuilderModule.class
  )
  @FragmentScoped
  public abstract static class FragmentC implements FragmentComponent,
      DefaultViewModelFactories.FragmentEntryPoint,
      ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint,
      GeneratedComponent,
      SftpSettingsFragment_GeneratedInjector,
      AboutFragment_GeneratedInjector,
      DeviceFragment_GeneratedInjector,
      PairingFragment_GeneratedInjector,
      PluginSettingsFragment_GeneratedInjector,
      PluginSettingsListFragment_GeneratedInjector,
      SettingsFragment_GeneratedInjector {
    @Subcomponent.Builder
    abstract interface Builder extends FragmentComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewWithFragmentC implements ViewWithFragmentComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewWithFragmentComponentBuilder {
    }
  }
}
