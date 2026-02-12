# Camera Streaming Performance Optimization

## Issue #110 - Performance Targets

This document describes the performance optimizations implemented for the camera webcam streaming feature.

## Performance Targets

| Metric | Target | Acceptable | Notes |
|--------|--------|------------|-------|
| End-to-end latency | <100ms | <150ms | From camera capture to V4L2 write |
| Frame rate | 30 fps stable | 25+ fps | Consistent without drops |
| Battery drain (Android) | <8%/hour | <10%/hour | During active streaming |
| Memory usage | <100MB | <150MB | Combined Android + Desktop |
| CPU usage (Android) | <15% | <25% | During 720p@30fps streaming |
| CPU usage (Desktop) | <5% | <10% | During 720p@30fps streaming |

## Android-Side Optimizations

### H264Encoder.kt

1. **CBR Mode for Streaming**
   - Uses Constant Bitrate (CBR) instead of VBR
   - Provides more consistent latency
   - Reduces encoder decision overhead

2. **Low Latency Flags**
   - `KEY_LOW_LATENCY = 1` (Android 11+)
   - `KEY_PRIORITY = PRIORITY_REALTIME` (Android 12+)
   - `KEY_MAX_B_FRAMES = 0` - No B-frames for lower latency

3. **Profile Selection**
   - Uses Constrained Baseline for best compatibility
   - Optimized for real-time streaming scenarios

### CameraCaptureService.kt

1. **Video Stabilization Disabled**
   - Saves ~30ms of latency from frame buffering
   - Can be re-enabled via settings if quality preferred

2. **Optimized Capture Request**
   - Fixed FPS range for consistent timing
   - Fast noise reduction mode
   - Fast edge enhancement mode

3. **Continuous Video Autofocus**
   - Optimized for video rather than still capture

### CameraStreamClient.kt

1. **Adaptive Bitrate**
   - Automatic bitrate reduction on congestion
   - Gradual recovery after stable period
   - Range: 500 kbps - 8000 kbps

2. **Reduced Queue Size**
   - `MAX_PENDING_FRAMES = 3` (was 5)
   - Faster detection of backpressure

3. **Faster Bandwidth Monitoring**
   - 500ms interval (was 1000ms)
   - Quicker congestion response

### CameraPerformanceMonitor.kt

New performance monitoring infrastructure:
- Real-time FPS calculation
- Encoding time tracking
- Network latency estimation
- Drop rate monitoring
- Performance status reporting

## Desktop-Side Optimizations

### camera_daemon.rs

1. **Reduced Queue Size**
   - `queue_size = 5` (was 10)
   - Lower end-to-end latency

2. **Shorter Timeout**
   - 50ms frame timeout (was 100ms)
   - Faster response to state changes

3. **Performance Monitoring**
   - Integrated `PerformanceMonitor`
   - Decode and write timing

### frame.rs

1. **Optimized Format Conversion**
   - Cache-friendly row-by-row processing
   - Direct indexing with `get_unchecked()`
   - Pre-allocated output buffers

### performance.rs

New performance monitoring module:
- Frame receive tracking
- Decode time measurement
- V4L2 write timing
- Rolling averages for metrics

## Measuring Performance

### Android Metrics

```kotlin
val monitor = CameraPerformanceMonitor()
monitor.start()

// During streaming
val metrics = monitor.getMetrics()
Log.d(TAG, """
    FPS: ${metrics.currentFps}
    Latency: ${metrics.avgLatencyMs}ms
    Encoding: ${metrics.avgEncodingTimeMs}ms
    Bitrate: ${metrics.bitrateKbps}kbps
    Status: ${metrics.getStatus()}
""")
```

### Desktop Metrics

```rust
let metrics = daemon.perf_monitor().map(|m| m.get_metrics());
if let Some(m) = metrics {
    info!("FPS: {:.1}, Decode: {:.1}ms, Write: {:.1}ms, Status: {:?}",
        m.current_fps, m.avg_decode_time_ms, m.avg_write_time_ms, m.get_status());
}
```

## Battery Optimization Tips

1. **Use Lower Resolution**
   - 720p uses ~30% less power than 1080p

2. **Reduce Frame Rate**
   - 24fps uses ~20% less power than 30fps

3. **Enable Low Latency Mode**
   - Disables stabilization which uses CPU

4. **Use Adaptive Bitrate**
   - Reduces encoding work during congestion

## Known Limitations

1. **No Hardware Decoding on Desktop**
   - OpenH264 is software-only
   - VAAPI/VDPAU support planned for future

2. **Format Conversion Overhead**
   - I420 to YUYV conversion required
   - ~5ms per frame at 720p

3. **Network Dependency**
   - Latency depends on WiFi quality
   - Use 5GHz band for best results

## Future Improvements

- [ ] Hardware-accelerated decoding (VAAPI/VDPAU)
- [ ] Triple buffering for V4L2
- [ ] Zero-copy frame pipeline
- [ ] H.265/HEVC support
- [ ] Audio streaming support
