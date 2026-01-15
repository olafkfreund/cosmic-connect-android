# COSMIC Desktop Development Skill

## Overview
This skill provides comprehensive guidance for developing COSMIC Desktop applications and applets using Rust, libcosmic, and the Wayland Layer Shell protocol.

## COSMIC Desktop Architecture

### libcosmic Framework
libcosmic is built on top of `iced`, providing COSMIC-specific widgets and patterns for desktop integration.

```rust
use cosmic::{
    app::{Core, Task},
    iced::{
        alignment::{Horizontal, Vertical},
        Length, Subscription,
    },
    widget, Application, Element,
};

// Basic COSMIC application structure
struct CosmicKdeConnect {
    core: Core,
    devices: Vec<DeviceInfo>,
    selected_device: Option<String>,
}

#[derive(Debug, Clone)]
enum Message {
    DeviceDiscovered(DeviceInfo),
    DeviceSelected(String),
    SendPing(String),
    Tick,
}

impl Application for CosmicKdeConnect {
    type Executor = cosmic::executor::Default;
    type Flags = ();
    type Message = Message;
    
    const APP_ID: &'static str = "com.system76.CosmicKdeConnect";
    
    fn core(&self) -> &Core {
        &self.core
    }
    
    fn core_mut(&mut self) -> &mut Core {
        &mut self.core
    }
    
    fn init(core: Core, _flags: Self::Flags) -> (Self, Task<Self::Message>) {
        let app = Self {
            core,
            devices: Vec::new(),
            selected_device: None,
        };
        
        (app, Task::none())
    }
    
    fn update(&mut self, message: Self::Message) -> Task<Self::Message> {
        match message {
            Message::DeviceDiscovered(device) => {
                self.devices.push(device);
                Task::none()
            }
            Message::DeviceSelected(id) => {
                self.selected_device = Some(id);
                Task::none()
            }
            Message::SendPing(device_id) => {
                // Handle ping
                Task::none()
            }
            Message::Tick => {
                // Periodic updates
                Task::none()
            }
        }
    }
    
    fn view(&self) -> Element<Self::Message> {
        widget::container(
            widget::column![
                widget::text("Connected Devices").size(24),
                self.device_list(),
                self.action_buttons(),
            ]
            .spacing(20)
            .padding(20)
        )
        .width(Length::Fill)
        .height(Length::Fill)
        .into()
    }
    
    fn subscription(&self) -> Subscription<Self::Message> {
        cosmic::iced::time::every(std::time::Duration::from_secs(1))
            .map(|_| Message::Tick)
    }
}
```

## COSMIC Applet Development

### Applet Structure
```rust
use cosmic::{
    iced::{
        wayland::popup::{destroy_popup, get_popup},
        window::Id as SurfaceId,
        Alignment, Length, Subscription,
    },
    iced_runtime::core::window,
    iced_style::application,
    widget, Application, Element,
};

pub struct CosmicKdeConnectApplet {
    core: cosmic::app::Core,
    popup: Option<SurfaceId>,
    devices: Vec<DeviceInfo>,
    dbus_connection: Option<Arc<zbus::Connection>>,
}

#[derive(Debug, Clone)]
pub enum Message {
    TogglePopup,
    PopupClosed(SurfaceId),
    DeviceListUpdated(Vec<DeviceInfo>),
    SendPing(String),
    ShareFile(String, PathBuf),
    PairDevice(String),
    UnpairDevice(String),
    DbusEvent(DbusMessage),
}

impl Application for CosmicKdeConnectApplet {
    type Executor = cosmic::executor::Default;
    type Flags = ();
    type Message = Message;
    
    const APP_ID: &'static str = "com.system76.CosmicAppletKdeConnect";
    
    fn core(&self) -> &cosmic::app::Core {
        &self.core
    }
    
    fn core_mut(&mut self) -> &mut cosmic::app::Core {
        &mut self.core
    }
    
    fn init(
        core: cosmic::app::Core,
        _flags: Self::Flags,
    ) -> (Self, cosmic::app::Task<Self::Message>) {
        let app = Self {
            core,
            popup: None,
            devices: Vec::new(),
            dbus_connection: None,
        };
        
        let task = cosmic::app::Task::perform(
            async { establish_dbus_connection().await },
            |conn| Message::DbusEvent(DbusMessage::Connected(conn)),
        );
        
        (app, task)
    }
    
    fn update(&mut self, message: Self::Message) -> cosmic::app::Task<Self::Message> {
        match message {
            Message::TogglePopup => {
                if let Some(id) = self.popup.take() {
                    destroy_popup(id)
                } else {
                    let new_id = SurfaceId::unique();
                    self.popup = Some(new_id);
                    
                    let popup_settings = self.core.applet.get_popup_settings(
                        window::Id::MAIN,
                        new_id,
                        None,
                        None,
                        None,
                    );
                    
                    get_popup(popup_settings)
                }
            }
            Message::PopupClosed(id) => {
                if self.popup == Some(id) {
                    self.popup = None;
                }
                cosmic::app::Task::none()
            }
            Message::DeviceListUpdated(devices) => {
                self.devices = devices;
                cosmic::app::Task::none()
            }
            Message::SendPing(device_id) => {
                self.send_dbus_command("SendPing", &device_id, "Hello!")
            }
            Message::ShareFile(device_id, path) => {
                self.send_dbus_file_share("ShareFile", &device_id, &path)
            }
            Message::PairDevice(device_id) => {
                self.send_dbus_command("RequestPairing", &device_id, "")
            }
            Message::UnpairDevice(device_id) => {
                self.send_dbus_command("UnpairDevice", &device_id, "")
            }
            Message::DbusEvent(event) => {
                self.handle_dbus_event(event)
            }
        }
    }
    
    fn view(&self) -> Element<Self::Message> {
        self.core
            .applet
            .icon_button("network-wireless-symbolic")
            .on_press(Message::TogglePopup)
            .into()
    }
    
    fn view_window(&self, id: SurfaceId) -> Element<Self::Message> {
        if Some(id) == self.popup {
            self.popup_view()
        } else {
            widget::text("Unknown window").into()
        }
    }
    
    fn subscription(&self) -> Subscription<Self::Message> {
        Subscription::batch(vec![
            self.dbus_subscription(),
            cosmic::iced::time::every(Duration::from_secs(5))
                .map(|_| Message::DbusEvent(DbusMessage::RefreshDevices)),
        ])
    }
}

impl CosmicKdeConnectApplet {
    fn popup_view(&self) -> Element<Message> {
        let device_list = widget::column(
            self.devices
                .iter()
                .map(|device| self.device_row(device))
                .collect::<Vec<_>>(),
        )
        .spacing(10);
        
        widget::container(
            widget::column![
                widget::text("KDE Connect Devices").size(18),
                widget::divider::horizontal::default(),
                device_list,
            ]
            .spacing(10)
            .padding(15),
        )
        .width(Length::Fixed(350.0))
        .style(cosmic::theme::Container::default())
        .into()
    }
    
    fn device_row(&self, device: &DeviceInfo) -> Element<Message> {
        widget::row![
            widget::icon::from_name(device.icon_name()).size(32),
            widget::column![
                widget::text(&device.name).size(14),
                widget::text(format!("Battery: {}%", device.battery_level)).size(12),
            ]
            .spacing(5),
            widget::button(widget::text("Ping"))
                .on_press(Message::SendPing(device.id.clone())),
            widget::button(widget::text("Share"))
                .on_press(Message::ToggleFileChooser(device.id.clone())),
        ]
        .spacing(10)
        .align_items(Alignment::Center)
        .into()
    }
}
```

## Wayland Layer Shell Integration

### Layer Shell Window Creation
```rust
use smithay_client_toolkit::{
    compositor::CompositorHandler,
    delegate_compositor, delegate_layer, delegate_output, delegate_registry, delegate_seat,
    output::{OutputHandler, OutputState},
    registry::{ProvidesRegistryState, RegistryState},
    registry_handlers,
    seat::{Capability, SeatHandler, SeatState},
    shell::{
        wlr_layer::{
            Anchor, KeyboardInteractivity, Layer, LayerShell, LayerShellHandler,
            LayerSurface, LayerSurfaceConfigure,
        },
        WaylandSurface,
    },
};

struct LayerShellWindow {
    registry_state: RegistryState,
    output_state: OutputState,
    seat_state: SeatState,
    layer_shell: LayerShell,
    layer_surface: Option<LayerSurface>,
    width: u32,
    height: u32,
}

impl LayerShellWindow {
    fn new() -> Self {
        let conn = wayland_client::Connection::connect_to_env().unwrap();
        let display = conn.display();
        
        let mut event_queue = conn.new_event_queue();
        let qh = event_queue.handle();
        
        let globals = display.get_registry(&qh, ());
        
        event_queue.blocking_dispatch(&mut AppData::new()).unwrap();
        
        Self {
            registry_state: RegistryState::new(&globals),
            output_state: OutputState::new(&globals, &qh),
            seat_state: SeatState::new(&globals, &qh),
            layer_shell: LayerShell::bind(&globals, &qh).unwrap(),
            layer_surface: None,
            width: 300,
            height: 200,
        }
    }
    
    fn create_layer_surface(&mut self, qh: &QueueHandle<Self>) {
        let compositor = self.registry_state.compositor();
        let surface = compositor.create_surface(qh);
        
        let layer_surface = self.layer_shell.create_layer_surface(
            qh,
            surface,
            Layer::Overlay,
            Some("kdeconnect-widget"),
            None,
        );
        
        layer_surface.set_anchor(Anchor::TOP | Anchor::RIGHT);
        layer_surface.set_size(self.width, self.height);
        layer_surface.set_keyboard_interactivity(KeyboardInteractivity::OnDemand);
        layer_surface.set_margin(10, 10, 10, 10);
        
        layer_surface.commit();
        
        self.layer_surface = Some(layer_surface);
    }
}

impl CompositorHandler for LayerShellWindow {
    fn scale_factor_changed(
        &mut self,
        _conn: &Connection,
        _qh: &QueueHandle<Self>,
        surface: &wl_surface::WlSurface,
        new_factor: i32,
    ) {
        // Handle scale changes
    }
    
    fn frame(
        &mut self,
        _conn: &Connection,
        qh: &QueueHandle<Self>,
        surface: &wl_surface::WlSurface,
        _time: u32,
    ) {
        // Render frame
        self.draw(surface);
        surface.frame(qh, surface.clone());
        surface.commit();
    }
}

impl LayerShellHandler for LayerShellWindow {
    fn closed(&mut self, _conn: &Connection, _qh: &QueueHandle<Self>, _layer: &LayerSurface) {
        // Handle closure
    }
    
    fn configure(
        &mut self,
        _conn: &Connection,
        qh: &QueueHandle<Self>,
        layer: &LayerSurface,
        configure: LayerSurfaceConfigure,
        _serial: u32,
    ) {
        if configure.new_size.0 != 0 {
            self.width = configure.new_size.0;
        }
        if configure.new_size.1 != 0 {
            self.height = configure.new_size.1;
        }
        
        // Acknowledge configure
        layer.ack_configure(configure.serial);
    }
}

delegate_compositor!(LayerShellWindow);
delegate_output!(LayerShellWindow);
delegate_seat!(LayerShellWindow);
delegate_layer!(LayerShellWindow);
delegate_registry!(LayerShellWindow);
```

## DBus Integration

### DBus Interface Definition
```rust
use zbus::{dbus_interface, Connection};
use std::sync::Arc;
use tokio::sync::RwLock;

pub struct KdeConnectService {
    devices: Arc<RwLock<HashMap<String, DeviceInfo>>>,
    daemon_connection: Arc<Connection>,
}

#[dbus_interface(name = "com.system76.CosmicKdeConnect")]
impl KdeConnectService {
    async fn get_devices(&self) -> Vec<DeviceInfo> {
        self.devices.read().await.values().cloned().collect()
    }
    
    async fn get_device(&self, device_id: String) -> zbus::fdo::Result<DeviceInfo> {
        self.devices
            .read()
            .await
            .get(&device_id)
            .cloned()
            .ok_or_else(|| zbus::fdo::Error::Failed("Device not found".to_string()))
    }
    
    async fn send_ping(&self, device_id: String, message: String) -> zbus::fdo::Result<()> {
        // Call daemon DBus method
        let proxy = DaemonProxyBuilder::new(&self.daemon_connection)
            .await
            .map_err(|e| zbus::fdo::Error::Failed(e.to_string()))?;
        
        proxy
            .send_ping(&device_id, &message)
            .await
            .map_err(|e| zbus::fdo::Error::Failed(e.to_string()))
    }
    
    async fn share_file(&self, device_id: String, path: String) -> zbus::fdo::Result<()> {
        let proxy = DaemonProxyBuilder::new(&self.daemon_connection)
            .await
            .map_err(|e| zbus::fdo::Error::Failed(e.to_string()))?;
        
        proxy
            .share_file(&device_id, &path)
            .await
            .map_err(|e| zbus::fdo::Error::Failed(e.to_string()))
    }
    
    #[dbus_interface(signal)]
    async fn device_discovered(signal_ctxt: &SignalContext<'_>, device_id: String) -> zbus::Result<()>;
    
    #[dbus_interface(signal)]
    async fn device_state_changed(
        signal_ctxt: &SignalContext<'_>,
        device_id: String,
        state: String,
    ) -> zbus::Result<()>;
}

// DBus proxy for daemon
#[dbus_proxy(
    interface = "com.system76.CosmicKdeConnect",
    default_service = "com.system76.CosmicKdeConnect",
    default_path = "/com/system76/CosmicKdeConnect"
)]
trait KdeConnectDaemon {
    async fn get_devices(&self) -> zbus::Result<Vec<DeviceInfo>>;
    async fn send_ping(&self, device_id: &str, message: &str) -> zbus::Result<()>;
    async fn share_file(&self, device_id: &str, path: &str) -> zbus::Result<()>;
    
    #[dbus_proxy(signal)]
    fn device_discovered(&self, device_id: String) -> zbus::Result<()>;
}
```

### DBus Subscription in Application
```rust
impl CosmicKdeConnectApplet {
    fn dbus_subscription(&self) -> Subscription<Message> {
        struct DbusWorker;
        
        subscription::channel(
            std::any::TypeId::of::<DbusWorker>(),
            100,
            |mut output| async move {
                let conn = Connection::session().await.unwrap();
                let proxy = KdeConnectDaemonProxyBuilder::new(&conn)
                    .await
                    .unwrap();
                
                // Subscribe to signals
                let mut device_discovered = proxy.receive_device_discovered().await.unwrap();
                
                loop {
                    tokio::select! {
                        Some(signal) = device_discovered.next() => {
                            let args = signal.args().unwrap();
                            let _ = output.send(Message::DbusEvent(
                                DbusMessage::DeviceDiscovered(args.device_id)
                            )).await;
                        }
                        _ = tokio::time::sleep(Duration::from_secs(5)) => {
                            // Periodic refresh
                            match proxy.get_devices().await {
                                Ok(devices) => {
                                    let _ = output.send(
                                        Message::DeviceListUpdated(devices)
                                    ).await;
                                }
                                Err(e) => {
                                    eprintln!("Failed to get devices: {}", e);
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
```

## COSMIC Notifications Integration

### Sending Notifications
```rust
use zbus::Connection;

pub struct NotificationService {
    connection: Arc<Connection>,
}

impl NotificationService {
    pub async fn new() -> zbus::Result<Self> {
        let connection = Connection::session().await?;
        Ok(Self {
            connection: Arc::new(connection),
        })
    }
    
    pub async fn send_notification(
        &self,
        title: &str,
        body: &str,
        icon: Option<&str>,
        urgency: Urgency,
    ) -> zbus::Result<u32> {
        let proxy = NotificationsProxyBuilder::new(&self.connection)
            .await?;
        
        let mut hints = HashMap::new();
        hints.insert("urgency", zbus::zvariant::Value::from(urgency as u8));
        
        proxy
            .notify(
                "cosmic-kdeconnect",
                0,
                icon.unwrap_or(""),
                title,
                body,
                vec![],
                hints,
                5000, // 5 seconds timeout
            )
            .await
    }
    
    pub async fn send_file_received_notification(
        &self,
        filename: &str,
        device_name: &str,
    ) -> zbus::Result<u32> {
        self.send_notification(
            "File Received",
            &format!("Received {} from {}", filename, device_name),
            Some("document-save"),
            Urgency::Normal,
        )
        .await
    }
    
    pub async fn send_low_battery_notification(
        &self,
        device_name: &str,
        battery_level: u8,
    ) -> zbus::Result<u32> {
        self.send_notification(
            "Low Battery",
            &format!("{} battery is at {}%", device_name, battery_level),
            Some("battery-low"),
            Urgency::Critical,
        )
        .await
    }
}

#[derive(Debug, Clone, Copy)]
pub enum Urgency {
    Low = 0,
    Normal = 1,
    Critical = 2,
}

#[dbus_proxy(
    interface = "org.freedesktop.Notifications",
    default_service = "org.freedesktop.Notifications",
    default_path = "/org/freedesktop/Notifications"
)]
trait Notifications {
    fn notify(
        &self,
        app_name: &str,
        replaces_id: u32,
        app_icon: &str,
        summary: &str,
        body: &str,
        actions: Vec<&str>,
        hints: HashMap<&str, zbus::zvariant::Value<'_>>,
        expire_timeout: i32,
    ) -> zbus::Result<u32>;
}
```

## File Picker Integration (XDG Desktop Portal)

### Opening File Picker
```rust
use ashpd::desktop::file_chooser::{Choice, FileFilter, OpenFileRequest};

pub async fn open_file_picker() -> Result<Vec<PathBuf>, ashpd::Error> {
    let request = OpenFileRequest::default()
        .title("Select file to share")
        .modal(true)
        .multiple(false)
        .filters(vec![
            FileFilter::new("All Files")
                .glob("*")
                .build(),
            FileFilter::new("Images")
                .mimetype("image/*")
                .build(),
            FileFilter::new("Documents")
                .mimetype("application/pdf")
                .mimetype("application/msword")
                .build(),
        ]);
    
    let response = request.send().await?.response()?;
    
    Ok(response.uris().iter().map(|uri| {
        PathBuf::from(uri.path())
    }).collect())
}

pub async fn save_file_picker(
    filename: &str,
) -> Result<Option<PathBuf>, ashpd::Error> {
    let request = ashpd::desktop::file_chooser::SaveFileRequest::default()
        .title("Save file")
        .current_name(filename)
        .modal(true);
    
    let response = request.send().await?.response()?;
    
    Ok(response.uris().first().map(|uri| {
        PathBuf::from(uri.path())
    }))
}
```

## MPRIS Media Control Integration

### MPRIS Player Discovery and Control
```rust
use mpris::{Metadata, Player, PlayerFinder};
use std::time::Duration;

pub struct MprisController {
    player_finder: PlayerFinder,
    current_player: Option<Player>,
}

impl MprisController {
    pub fn new() -> Result<Self, mpris::FindingError> {
        Ok(Self {
            player_finder: PlayerFinder::new()?,
            current_player: None,
        })
    }
    
    pub fn list_players(&self) -> Result<Vec<String>, mpris::FindingError> {
        Ok(self
            .player_finder
            .find_all()?
            .iter()
            .map(|p| p.identity().to_string())
            .collect())
    }
    
    pub fn get_player(&mut self, name: &str) -> Result<(), mpris::FindingError> {
        let player = self
            .player_finder
            .find_by_name(name)
            .or_else(|_| self.player_finder.find_active())?;
        
        self.current_player = Some(player);
        Ok(())
    }
    
    pub fn play_pause(&self) -> Result<(), mpris::DBusError> {
        if let Some(player) = &self.current_player {
            player.play_pause()
        } else {
            Err(mpris::DBusError::Miscellaneous(
                "No player selected".to_string(),
            ))
        }
    }
    
    pub fn next(&self) -> Result<(), mpris::DBusError> {
        if let Some(player) = &self.current_player {
            player.next()
        } else {
            Err(mpris::DBusError::Miscellaneous(
                "No player selected".to_string(),
            ))
        }
    }
    
    pub fn previous(&self) -> Result<(), mpris::DBusError> {
        if let Some(player) = &self.current_player {
            player.previous()
        } else {
            Err(mpris::DBusError::Miscellaneous(
                "No player selected".to_string(),
            ))
        }
    }
    
    pub fn set_volume(&self, volume: f64) -> Result<(), mpris::DBusError> {
        if let Some(player) = &self.current_player {
            player.set_volume(volume.clamp(0.0, 1.0))
        } else {
            Err(mpris::DBusError::Miscellaneous(
                "No player selected".to_string(),
            ))
        }
    }
    
    pub fn seek(&self, offset: Duration) -> Result<(), mpris::DBusError> {
        if let Some(player) = &self.current_player {
            player.seek_forwards(offset)
        } else {
            Err(mpris::DBusError::Miscellaneous(
                "No player selected".to_string(),
            ))
        }
    }
    
    pub fn get_metadata(&self) -> Option<Metadata> {
        self.current_player.as_ref()
            .and_then(|p| p.get_metadata().ok())
    }
}
```

## Error Handling

### Custom Error Types
```rust
use thiserror::Error;

#[derive(Error, Debug)]
pub enum CosmicKdeConnectError {
    #[error("DBus error: {0}")]
    Dbus(#[from] zbus::Error),
    
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    
    #[error("Device not found: {0}")]
    DeviceNotFound(String),
    
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Protocol error: {0}")]
    Protocol(String),
    
    #[error("Notification error: {0}")]
    Notification(String),
}

pub type Result<T> = std::result::Result<T, CosmicKdeConnectError>;
```

## Testing COSMIC Applications

### Integration Tests
```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[tokio::test]
    async fn test_dbus_connection() {
        let conn = Connection::session().await.unwrap();
        let proxy = KdeConnectDaemonProxyBuilder::new(&conn)
            .await
            .unwrap();
        
        let devices = proxy.get_devices().await.unwrap();
        assert!(devices.is_empty() || !devices.is_empty());
    }
    
    #[test]
    fn test_device_info_serialization() {
        let device = DeviceInfo {
            id: "test-device".to_string(),
            name: "Test Device".to_string(),
            device_type: DeviceType::Phone,
            is_connected: true,
            battery_level: 85,
            is_charging: false,
        };
        
        let json = serde_json::to_string(&device).unwrap();
        let deserialized: DeviceInfo = serde_json::from_str(&json).unwrap();
        
        assert_eq!(device, deserialized);
    }
}
```

## Cargo Dependencies

### Cargo.toml for COSMIC Applet
```toml
[package]
name = "cosmic-applet-kdeconnect"
version = "0.1.0"
edition = "2021"

[dependencies]
cosmic = { git = "https://github.com/pop-os/libcosmic" }
zbus = "3.15"
tokio = { version = "1.35", features = ["full"] }
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
thiserror = "1.0"
ashpd = "0.7"
mpris = "2.0"
tracing = "0.1"
tracing-subscriber = "0.3"

[dependencies.smithay-client-toolkit]
version = "0.18"
features = ["calloop"]

[dependencies.wayland-client]
version = "0.31"
```

## Building and Running

### Development Build
```bash
# Build the applet
cargo build --bin cosmic-applet-kdeconnect

# Run with logging
RUST_LOG=debug cargo run --bin cosmic-applet-kdeconnect

# Run tests
cargo test
```

### Installation
```bash
# Build release
cargo build --release --bin cosmic-applet-kdeconnect

# Install system-wide
sudo install -Dm755 target/release/cosmic-applet-kdeconnect \
    /usr/bin/cosmic-applet-kdeconnect

# Install desktop file
sudo install -Dm644 data/cosmic-applet-kdeconnect.desktop \
    /usr/share/applications/cosmic-applet-kdeconnect.desktop
```

## Best Practices

1. **Use async/await consistently**: All I/O operations should be async
2. **Proper error propagation**: Use `?` operator and Result types
3. **Resource cleanup**: Implement Drop traits for cleanup
4. **State management**: Use Arc<RwLock<T>> for shared state
5. **Logging**: Use tracing crate for structured logging
6. **Testing**: Write unit and integration tests
7. **Documentation**: Document public APIs with rustdoc
8. **Performance**: Profile with cargo-flamegraph

## Resources

- [libcosmic Documentation](https://pop-os.github.io/libcosmic/)
- [COSMIC Desktop GitHub](https://github.com/pop-os/cosmic-epoch)
- [Wayland Protocol](https://wayland.freedesktop.org/)
- [Layer Shell Protocol](https://github.com/swaywm/wlr-protocols/blob/master/unstable/wlr-layer-shell-unstable-v1.xml)
- [zbus Documentation](https://docs.rs/zbus/)
- [XDG Desktop Portal](https://flatpak.github.io/xdg-desktop-portal/)
