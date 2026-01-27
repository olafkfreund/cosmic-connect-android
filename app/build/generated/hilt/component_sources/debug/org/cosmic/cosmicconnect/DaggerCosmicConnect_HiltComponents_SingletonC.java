package org.cosmic.cosmicconnect;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.cosmic.cosmicconnect.Backends.BluetoothBackend.BluetoothLinkProvider;
import org.cosmic.cosmicconnect.Backends.LanBackend.LanLinkProvider;
import org.cosmic.cosmicconnect.Core.DeviceRegistry;
import org.cosmic.cosmicconnect.Helpers.DeviceHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.RsaHelper;
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper;
import org.cosmic.cosmicconnect.Helpers.TrustedDevices;
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.DigitizerActivity;
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneActivity;
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneReceiver;
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhoneReceiver_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.ComposeSendActivity;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.ComposeSendActivity_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadActivity;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadActivity_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.SendKeystrokesToHostActivity;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisActivity;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaNotificationReceiver;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaNotificationReceiver_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaSession;
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationFilterActivity;
import org.cosmic.cosmicconnect.Plugins.PluginFactory;
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity;
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.RunCommandPlugin.RunCommandActivity;
import org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpSettingsFragment;
import org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpSettingsFragment_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.SendFileActivity;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.SendFileActivity_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareActivity;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareActivity_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareBroadcastReceiver;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareBroadcastReceiver_MembersInjector;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareChooserTargetService;
import org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareChooserTargetService_MembersInjector;
import org.cosmic.cosmicconnect.UserInterface.About.AboutFragment;
import org.cosmic.cosmicconnect.UserInterface.About.LicensesActivity;
import org.cosmic.cosmicconnect.UserInterface.CustomDevicesActivity;
import org.cosmic.cosmicconnect.UserInterface.DeviceFragment;
import org.cosmic.cosmicconnect.UserInterface.MainActivity;
import org.cosmic.cosmicconnect.UserInterface.MainActivity_MembersInjector;
import org.cosmic.cosmicconnect.UserInterface.MainViewModel;
import org.cosmic.cosmicconnect.UserInterface.MainViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.PairingFragment;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity_MembersInjector;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment_MembersInjector;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsListFragment;
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsListFragment_MembersInjector;
import org.cosmic.cosmicconnect.UserInterface.SettingsFragment;
import org.cosmic.cosmicconnect.UserInterface.TrustedNetworksActivity;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel_HiltModules;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel;
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel_HiltModules;

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
public final class DaggerCosmicConnect_HiltComponents_SingletonC {
  private DaggerCosmicConnect_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CosmicConnect_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CosmicConnect_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CosmicConnect_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CosmicConnect_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CosmicConnect_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CosmicConnect_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CosmicConnect_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CosmicConnect_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CosmicConnect_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CosmicConnect_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CosmicConnect_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }

    @Override
    public void injectSftpSettingsFragment(SftpSettingsFragment arg0) {
      injectSftpSettingsFragment2(arg0);
    }

    @Override
    public void injectAboutFragment(AboutFragment arg0) {
    }

    @Override
    public void injectDeviceFragment(DeviceFragment arg0) {
    }

    @Override
    public void injectPairingFragment(PairingFragment arg0) {
    }

    @Override
    public void injectPluginSettingsFragment(PluginSettingsFragment arg0) {
      injectPluginSettingsFragment2(arg0);
    }

    @Override
    public void injectPluginSettingsListFragment(PluginSettingsListFragment arg0) {
      injectPluginSettingsListFragment2(arg0);
    }

    @Override
    public void injectSettingsFragment(SettingsFragment arg0) {
    }

    @CanIgnoreReturnValue
    private SftpSettingsFragment injectSftpSettingsFragment2(SftpSettingsFragment instance) {
      PluginSettingsFragment_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      PluginSettingsFragment_MembersInjector.injectPluginFactory(instance, singletonCImpl.pluginFactoryProvider.get());
      SftpSettingsFragment_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PluginSettingsFragment injectPluginSettingsFragment2(PluginSettingsFragment instance) {
      PluginSettingsFragment_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      PluginSettingsFragment_MembersInjector.injectPluginFactory(instance, singletonCImpl.pluginFactoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PluginSettingsListFragment injectPluginSettingsListFragment2(
        PluginSettingsListFragment instance) {
      PluginSettingsListFragment_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }
  }

  private static final class ViewCImpl extends CosmicConnect_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CosmicConnect_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(17).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel, AboutViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel, CustomDevicesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel, DeviceDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel, DeviceListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel, DigitizerViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel, FindMyPhoneViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel, LicensesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_MainViewModel, MainViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel, MousePadViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel, MprisViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel, NotificationFilterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel, PluginSettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel, RunCommandViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel, SendKeystrokesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel, ShareViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel, TrustedNetworksViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public void injectDigitizerActivity(DigitizerActivity arg0) {
    }

    @Override
    public void injectFindMyPhoneActivity(FindMyPhoneActivity arg0) {
    }

    @Override
    public void injectComposeSendActivity(ComposeSendActivity arg0) {
      injectComposeSendActivity2(arg0);
    }

    @Override
    public void injectMousePadActivity(MousePadActivity arg0) {
      injectMousePadActivity2(arg0);
    }

    @Override
    public void injectSendKeystrokesToHostActivity(SendKeystrokesToHostActivity arg0) {
    }

    @Override
    public void injectMprisActivity(MprisActivity arg0) {
    }

    @Override
    public void injectNotificationFilterActivity(NotificationFilterActivity arg0) {
    }

    @Override
    public void injectPresenterActivity(PresenterActivity arg0) {
      injectPresenterActivity2(arg0);
    }

    @Override
    public void injectRunCommandActivity(RunCommandActivity arg0) {
    }

    @Override
    public void injectSendFileActivity(SendFileActivity arg0) {
      injectSendFileActivity2(arg0);
    }

    @Override
    public void injectShareActivity(ShareActivity arg0) {
      injectShareActivity2(arg0);
    }

    @Override
    public void injectLicensesActivity(LicensesActivity arg0) {
    }

    @Override
    public void injectCustomDevicesActivity(CustomDevicesActivity arg0) {
    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public void injectPluginSettingsActivity(PluginSettingsActivity arg0) {
      injectPluginSettingsActivity2(arg0);
    }

    @Override
    public void injectTrustedNetworksActivity(TrustedNetworksActivity arg0) {
    }

    @CanIgnoreReturnValue
    private ComposeSendActivity injectComposeSendActivity2(ComposeSendActivity instance) {
      ComposeSendActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private MousePadActivity injectMousePadActivity2(MousePadActivity instance) {
      MousePadActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PresenterActivity injectPresenterActivity2(PresenterActivity instance) {
      PresenterActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private SendFileActivity injectSendFileActivity2(SendFileActivity instance) {
      SendFileActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ShareActivity injectShareActivity2(ShareActivity instance) {
      ShareActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      MainActivity_MembersInjector.injectDeviceHelper(instance, singletonCImpl.deviceHelperProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PluginSettingsActivity injectPluginSettingsActivity2(PluginSettingsActivity instance) {
      PluginSettingsActivity_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_MainViewModel = "org.cosmic.cosmicconnect.UserInterface.MainViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel";

      @KeepFieldType
      DeviceListViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel2;

      @KeepFieldType
      SendKeystrokesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel2;

      @KeepFieldType
      MprisViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel2;

      @KeepFieldType
      ShareViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel2;

      @KeepFieldType
      MainViewModel org_cosmic_cosmicconnect_UserInterface_MainViewModel2;

      @KeepFieldType
      AboutViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel2;

      @KeepFieldType
      CustomDevicesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel2;

      @KeepFieldType
      SettingsViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel2;

      @KeepFieldType
      PluginSettingsViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel2;

      @KeepFieldType
      FindMyPhoneViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel2;

      @KeepFieldType
      MousePadViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel2;

      @KeepFieldType
      RunCommandViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel2;

      @KeepFieldType
      TrustedNetworksViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel2;

      @KeepFieldType
      DeviceDetailViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel2;

      @KeepFieldType
      LicensesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel2;

      @KeepFieldType
      DigitizerViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel2;

      @KeepFieldType
      NotificationFilterViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel2;
    }
  }

  private static final class ViewModelCImpl extends CosmicConnect_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AboutViewModel> aboutViewModelProvider;

    private Provider<CustomDevicesViewModel> customDevicesViewModelProvider;

    private Provider<DeviceDetailViewModel> deviceDetailViewModelProvider;

    private Provider<DeviceListViewModel> deviceListViewModelProvider;

    private Provider<DigitizerViewModel> digitizerViewModelProvider;

    private Provider<FindMyPhoneViewModel> findMyPhoneViewModelProvider;

    private Provider<LicensesViewModel> licensesViewModelProvider;

    private Provider<MainViewModel> mainViewModelProvider;

    private Provider<MousePadViewModel> mousePadViewModelProvider;

    private Provider<MprisViewModel> mprisViewModelProvider;

    private Provider<NotificationFilterViewModel> notificationFilterViewModelProvider;

    private Provider<PluginSettingsViewModel> pluginSettingsViewModelProvider;

    private Provider<RunCommandViewModel> runCommandViewModelProvider;

    private Provider<SendKeystrokesViewModel> sendKeystrokesViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<ShareViewModel> shareViewModelProvider;

    private Provider<TrustedNetworksViewModel> trustedNetworksViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.aboutViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.customDevicesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.deviceDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.deviceListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.digitizerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.findMyPhoneViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.licensesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.mainViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.mousePadViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.mprisViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.notificationFilterViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.pluginSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.runCommandViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.sendKeystrokesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.shareViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.trustedNetworksViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(17).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel, ((Provider) aboutViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel, ((Provider) customDevicesViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel, ((Provider) deviceDetailViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel, ((Provider) deviceListViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel, ((Provider) digitizerViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel, ((Provider) findMyPhoneViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel, ((Provider) licensesViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_MainViewModel, ((Provider) mainViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel, ((Provider) mousePadViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel, ((Provider) mprisViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel, ((Provider) notificationFilterViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel, ((Provider) pluginSettingsViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel, ((Provider) runCommandViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel, ((Provider) sendKeystrokesViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel, ((Provider) shareViewModelProvider)).put(LazyClassKeyProvider.org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel, ((Provider) trustedNetworksViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_MainViewModel = "org.cosmic.cosmicconnect.UserInterface.MainViewModel";

      static String org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel = "org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel";

      @KeepFieldType
      DeviceListViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceListViewModel2;

      @KeepFieldType
      MousePadViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MousePadViewModel2;

      @KeepFieldType
      SendKeystrokesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_SendKeystrokesViewModel2;

      @KeepFieldType
      NotificationFilterViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_NotificationFilterViewModel2;

      @KeepFieldType
      DigitizerViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_DigitizerViewModel2;

      @KeepFieldType
      CustomDevicesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_CustomDevicesViewModel2;

      @KeepFieldType
      FindMyPhoneViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_FindMyPhoneViewModel2;

      @KeepFieldType
      MprisViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_MprisViewModel2;

      @KeepFieldType
      SettingsViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_SettingsViewModel2;

      @KeepFieldType
      DeviceDetailViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_DeviceDetailViewModel2;

      @KeepFieldType
      RunCommandViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_RunCommandViewModel2;

      @KeepFieldType
      LicensesViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_about_LicensesViewModel2;

      @KeepFieldType
      PluginSettingsViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_PluginSettingsViewModel2;

      @KeepFieldType
      AboutViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_about_AboutViewModel2;

      @KeepFieldType
      TrustedNetworksViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_config_TrustedNetworksViewModel2;

      @KeepFieldType
      MainViewModel org_cosmic_cosmicconnect_UserInterface_MainViewModel2;

      @KeepFieldType
      ShareViewModel org_cosmic_cosmicconnect_UserInterface_compose_screens_plugins_ShareViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // org.cosmic.cosmicconnect.UserInterface.compose.screens.about.AboutViewModel 
          return (T) new AboutViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel 
          return (T) new CustomDevicesViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceDetailViewModel 
          return (T) new DeviceDetailViewModel(singletonCImpl.deviceRegistryProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // org.cosmic.cosmicconnect.UserInterface.compose.screens.DeviceListViewModel 
          return (T) new DeviceListViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get());

          case 4: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel 
          return (T) new DigitizerViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get());

          case 5: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.FindMyPhoneViewModel 
          return (T) new FindMyPhoneViewModel(singletonCImpl.deviceRegistryProvider.get());

          case 6: // org.cosmic.cosmicconnect.UserInterface.compose.screens.about.LicensesViewModel 
          return (T) new LicensesViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // org.cosmic.cosmicconnect.UserInterface.MainViewModel 
          return (T) new MainViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get(), singletonCImpl.deviceHelperProvider.get());

          case 8: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel 
          return (T) new MousePadViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get());

          case 9: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MprisViewModel 
          return (T) new MprisViewModel(singletonCImpl.deviceRegistryProvider.get());

          case 10: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.NotificationFilterViewModel 
          return (T) new NotificationFilterViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel 
          return (T) new PluginSettingsViewModel(singletonCImpl.deviceRegistryProvider.get(), singletonCImpl.pluginFactoryProvider.get());

          case 12: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel 
          return (T) new RunCommandViewModel(singletonCImpl.deviceRegistryProvider.get());

          case 13: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel 
          return (T) new SendKeystrokesViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get());

          case 14: // org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel 
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel 
          return (T) new ShareViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceRegistryProvider.get());

          case 16: // org.cosmic.cosmicconnect.UserInterface.compose.screens.config.TrustedNetworksViewModel 
          return (T) new TrustedNetworksViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CosmicConnect_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CosmicConnect_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectBackgroundService(BackgroundService arg0) {
      injectBackgroundService2(arg0);
    }

    @Override
    public void injectShareChooserTargetService(ShareChooserTargetService arg0) {
      injectShareChooserTargetService2(arg0);
    }

    @CanIgnoreReturnValue
    private BackgroundService injectBackgroundService2(BackgroundService instance) {
      BackgroundService_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      BackgroundService_MembersInjector.injectLanLinkProvider(instance, singletonCImpl.lanLinkProvider.get());
      BackgroundService_MembersInjector.injectBluetoothLinkProvider(instance, singletonCImpl.bluetoothLinkProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ShareChooserTargetService injectShareChooserTargetService2(
        ShareChooserTargetService instance) {
      ShareChooserTargetService_MembersInjector.injectDeviceRegistry(instance, singletonCImpl.deviceRegistryProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends CosmicConnect_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<RsaHelper> rsaHelperProvider;

    private Provider<SslHelper> sslHelperProvider;

    private Provider<PluginFactory> pluginFactoryProvider;

    private Provider<DeviceHelper> deviceHelperProvider;

    private Provider<DeviceRegistry> deviceRegistryProvider;

    private Provider<MprisMediaSession> mprisMediaSessionProvider;

    private Provider<TrustedDevices> trustedDevicesProvider;

    private Provider<LanLinkProvider> lanLinkProvider;

    private Provider<BluetoothLinkProvider> bluetoothLinkProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.rsaHelperProvider = DoubleCheck.provider(new SwitchingProvider<RsaHelper>(singletonCImpl, 2));
      this.sslHelperProvider = DoubleCheck.provider(new SwitchingProvider<SslHelper>(singletonCImpl, 1));
      this.pluginFactoryProvider = DoubleCheck.provider(new SwitchingProvider<PluginFactory>(singletonCImpl, 4));
      this.deviceHelperProvider = DoubleCheck.provider(new SwitchingProvider<DeviceHelper>(singletonCImpl, 3));
      this.deviceRegistryProvider = DoubleCheck.provider(new SwitchingProvider<DeviceRegistry>(singletonCImpl, 0));
      this.mprisMediaSessionProvider = DoubleCheck.provider(new SwitchingProvider<MprisMediaSession>(singletonCImpl, 5));
      this.trustedDevicesProvider = DoubleCheck.provider(new SwitchingProvider<TrustedDevices>(singletonCImpl, 6));
      this.lanLinkProvider = DoubleCheck.provider(new SwitchingProvider<LanLinkProvider>(singletonCImpl, 7));
      this.bluetoothLinkProvider = DoubleCheck.provider(new SwitchingProvider<BluetoothLinkProvider>(singletonCImpl, 8));
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @Override
    public void injectCosmicConnect(CosmicConnect arg0) {
      injectCosmicConnect2(arg0);
    }

    @Override
    public void injectFindMyPhoneReceiver(FindMyPhoneReceiver arg0) {
      injectFindMyPhoneReceiver2(arg0);
    }

    @Override
    public void injectMprisMediaNotificationReceiver(MprisMediaNotificationReceiver arg0) {
      injectMprisMediaNotificationReceiver2(arg0);
    }

    @Override
    public void injectShareBroadcastReceiver(ShareBroadcastReceiver arg0) {
      injectShareBroadcastReceiver2(arg0);
    }

    @Override
    public DeviceRegistry deviceRegistry() {
      return deviceRegistryProvider.get();
    }

    @Override
    public DeviceHelper deviceHelper() {
      return deviceHelperProvider.get();
    }

    @Override
    public RsaHelper rsaHelper() {
      return rsaHelperProvider.get();
    }

    @Override
    public SslHelper sslHelper() {
      return sslHelperProvider.get();
    }

    @Override
    public PluginFactory pluginFactory() {
      return pluginFactoryProvider.get();
    }

    @Override
    public MprisMediaSession mprisMediaSession() {
      return mprisMediaSessionProvider.get();
    }

    @Override
    public TrustedDevices trustedDevices() {
      return trustedDevicesProvider.get();
    }

    @CanIgnoreReturnValue
    private CosmicConnect injectCosmicConnect2(CosmicConnect instance) {
      CosmicConnect_MembersInjector.injectDeviceRegistry(instance, deviceRegistryProvider.get());
      CosmicConnect_MembersInjector.injectDeviceHelper(instance, deviceHelperProvider.get());
      CosmicConnect_MembersInjector.injectRsaHelper(instance, rsaHelperProvider.get());
      CosmicConnect_MembersInjector.injectSslHelper(instance, sslHelperProvider.get());
      CosmicConnect_MembersInjector.injectPluginFactory(instance, pluginFactoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private FindMyPhoneReceiver injectFindMyPhoneReceiver2(FindMyPhoneReceiver instance) {
      FindMyPhoneReceiver_MembersInjector.injectDeviceRegistry(instance, deviceRegistryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private MprisMediaNotificationReceiver injectMprisMediaNotificationReceiver2(
        MprisMediaNotificationReceiver instance) {
      MprisMediaNotificationReceiver_MembersInjector.injectDeviceRegistry(instance, deviceRegistryProvider.get());
      MprisMediaNotificationReceiver_MembersInjector.injectMprisMediaSession(instance, mprisMediaSessionProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ShareBroadcastReceiver injectShareBroadcastReceiver2(ShareBroadcastReceiver instance) {
      ShareBroadcastReceiver_MembersInjector.injectDeviceRegistry(instance, deviceRegistryProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // org.cosmic.cosmicconnect.Core.DeviceRegistry 
          return (T) new DeviceRegistry(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.sslHelperProvider.get(), singletonCImpl.deviceHelperProvider.get(), singletonCImpl.pluginFactoryProvider.get());

          case 1: // org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper 
          return (T) new SslHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.rsaHelperProvider.get());

          case 2: // org.cosmic.cosmicconnect.Helpers.SecurityHelpers.RsaHelper 
          return (T) new RsaHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // org.cosmic.cosmicconnect.Helpers.DeviceHelper 
          return (T) new DeviceHelper(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.sslHelperProvider.get(), singletonCImpl.pluginFactoryProvider.get());

          case 4: // org.cosmic.cosmicconnect.Plugins.PluginFactory 
          return (T) new PluginFactory(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaSession 
          return (T) new MprisMediaSession();

          case 6: // org.cosmic.cosmicconnect.Helpers.TrustedDevices 
          return (T) new TrustedDevices(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // org.cosmic.cosmicconnect.Backends.LanBackend.LanLinkProvider 
          return (T) new LanLinkProvider(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceHelperProvider.get(), singletonCImpl.sslHelperProvider.get());

          case 8: // org.cosmic.cosmicconnect.Backends.BluetoothBackend.BluetoothLinkProvider 
          return (T) new BluetoothLinkProvider(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.deviceHelperProvider.get(), singletonCImpl.sslHelperProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
