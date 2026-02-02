# H.264 Video Decoder Implementation (Issue #138)

## Overview

Hardware-accelerated H.264 video decoder implementation for the Extended Display Plugin using Android MediaCodec API. Designed for low-latency real-time screen streaming from COSMIC Desktop to Android devices.

## Components

### 1. VideoFrame.kt
Data class representing a single encoded video frame.

**Key Features:**
- Holds H.264 NAL unit data
- Presentation timestamp (PTS) for frame ordering
- Key frame flag (I-frame vs P/B-frame)
- Optional dimensions for validation
- Proper equals/hashCode for ByteArray comparison

**Usage:**
```kotlin
val frame = VideoFrame(
    data = encodedH264Data,
    presentationTimeUs = System.nanoTime() / 1000,
    isKeyFrame = true,
    width = 1920,
    height = 1080
)
```

### 2. DecoderConfig.kt
Configuration for the H.264 decoder.

**Key Features:**
- Resolution configuration (width/height)
- Low-latency mode for real-time streaming
- Color format (Surface rendering)
- Maximum input buffer size
- Frame rate hints
- Preset configurations (720p, 1080p, 4K)

**Usage:**
```kotlin
// Use preset
val config = DecoderConfig.create1080p(lowLatency = true)

// Custom configuration
val config = DecoderConfig(
    width = 1920,
    height = 1080,
    lowLatencyMode = true,
    frameRate = 60
)
```

### 3. DecoderCallback.kt
Asynchronous callback for MediaCodec operations.

**Key Features:**
- Implements MediaCodec.Callback for async operation
- Input buffer queue management
- Output buffer rendering to Surface
- Frame statistics (decoded frames, dropped frames, latency)
- Thread-safe buffer queue
- Error handling and logging

**Key Methods:**
- `queueFrame(frame: VideoFrame)` - Queue encoded frame for decoding
- `getStatistics()` - Get decoder performance stats
- `hasAvailableInputBuffers()` - Check buffer availability

### 4. VideoDecoder.kt
Main hardware video decoder class.

**Key Features:**
- Hardware-accelerated H.264 decoding
- Low-latency mode for real-time streaming
- Thread-safe design (async frame feeding)
- Renders directly to Android Surface
- Comprehensive error handling
- Performance statistics tracking
- State management (IDLE, RUNNING, ERROR, STOPPED)

**Key Methods:**
- `start()` - Initialize and start decoder
- `decodeFrame(frame: VideoFrame)` - Decode a single frame
- `stop()` - Stop decoder and release resources
- `flush()` - Flush pending frames
- `reset()` - Reset to initial state
- `getStatistics()` - Get performance metrics

## Complete Usage Example

```kotlin
import android.view.Surface
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.*

class VideoStreamReceiver(private val surface: Surface) {

    private var decoder: VideoDecoder? = null
    private var frameCounter = 0L

    fun start(width: Int, height: Int) {
        // Create decoder configuration
        val config = DecoderConfig(
            width = width,
            height = height,
            lowLatencyMode = true,
            frameRate = 30
        )

        // Create and configure decoder
        decoder = VideoDecoder(config, surface).apply {
            // Set event callbacks
            setEventCallback(object : VideoDecoder.EventCallback {
                override fun onFrameRendered(presentationTimeUs: Long) {
                    Log.d(TAG, "Frame rendered: $presentationTimeUs")
                }

                override fun onError(error: Exception) {
                    Log.e(TAG, "Decoder error", error)
                }

                override fun onDecoderStarted() {
                    Log.i(TAG, "Decoder started")
                }

                override fun onDecoderStopped() {
                    Log.i(TAG, "Decoder stopped")
                }
            })

            // Start the decoder
            start()
        }
    }

    // Called when encoded frame arrives from network
    fun onEncodedFrameReceived(data: ByteArray, isKeyFrame: Boolean) {
        val frame = VideoFrame(
            data = data,
            presentationTimeUs = System.nanoTime() / 1000,
            isKeyFrame = isKeyFrame
        )

        val queued = decoder?.decodeFrame(frame) ?: false
        if (!queued) {
            Log.w(TAG, "Frame dropped - no input buffer available")
        }

        frameCounter++
    }

    fun getPerformanceStats() {
        decoder?.getStatistics()?.let { stats ->
            Log.i(TAG, """
                Decoder Statistics:
                - Frames decoded: ${stats.framesDecoded}
                - Frames dropped: ${stats.framesDropped}
                - Average latency: ${stats.averageLatencyMs}ms
                - Last frame PTS: ${stats.lastFrameTimeUs}
            """.trimIndent())
        }
    }

    fun stop() {
        decoder?.stop()
        decoder = null
    }
}
```

## Integration with Extended Display Plugin

The decoder is designed to integrate with the existing Extended Display Plugin:

1. **Network Layer** - Receives H.264 encoded frames via WebRTC data channel
2. **Signaling** - Negotiates video parameters (resolution, codec) during connection setup
3. **Rendering** - Outputs decoded frames to Surface for display
4. **Touch Input** - Sends touch events back to COSMIC Desktop

## Performance Characteristics

### Low-Latency Mode
- `KEY_LOW_LATENCY = 1` - Minimizes decoder buffering
- `KEY_PRIORITY = 0` - Real-time priority scheduling
- `KEY_OPERATING_RATE = Short.MAX_VALUE` - Maximize throughput

### Hardware Acceleration
- Uses device hardware decoder (GPU/DSP)
- Zero-copy rendering to Surface
- Supports up to 4K resolution (device dependent)

### Statistics Tracking
- Frames decoded/dropped count
- Average decode latency (microseconds)
- Last frame presentation timestamp
- Real-time performance monitoring

## Error Handling

The decoder handles several error scenarios:

1. **Codec Creation Failed** - Falls back gracefully, reports error
2. **Buffer Overflow** - Drops frames when input buffers full
3. **Codec Error** - Reports via callback, distinguishes recoverable/fatal errors
4. **Resource Exhaustion** - Proper cleanup and resource release

## Testing Recommendations

### Unit Tests
```kotlin
@Test
fun testDecoderLifecycle() {
    val config = DecoderConfig.create720p()
    val decoder = VideoDecoder(config, mockSurface)

    decoder.start()
    assertTrue(decoder.isRunning())

    decoder.stop()
    assertFalse(decoder.isRunning())
}

@Test
fun testFrameDecoding() {
    val frame = VideoFrame(
        data = sampleH264Frame,
        presentationTimeUs = 1000,
        isKeyFrame = true
    )

    val success = decoder.decodeFrame(frame)
    assertTrue(success)
}
```

### Integration Tests
- Test with real H.264 encoded video stream
- Verify latency under various network conditions
- Test resolution changes during streaming
- Verify memory usage stays bounded

### Performance Tests
- Measure decode latency (target: <16ms for 60fps)
- Monitor dropped frame rate (target: <1%)
- Test maximum resolution supported by device
- Verify CPU/battery usage

## Dependencies

Requires Android API level 21+ (Lollipop):
- `android.media.MediaCodec` - Hardware video decoder
- `android.media.MediaFormat` - Codec configuration
- `android.view.Surface` - Output rendering target

No additional third-party dependencies required.

## Future Enhancements

1. **Adaptive Bitrate** - Adjust quality based on network conditions
2. **Multiple Codec Support** - H.265, VP9, AV1
3. **Software Fallback** - Use software decoder if hardware unavailable
4. **Frame Interpolation** - Smooth playback during frame drops
5. **HDR Support** - High dynamic range video streaming

## References

- [Android MediaCodec Guide](https://developer.android.com/reference/android/media/MediaCodec)
- [Low-Latency Video Decoding](https://source.android.com/docs/core/media/low-latency)
- [H.264 NAL Unit Types](https://www.itu.int/rec/T-REC-H.264)

## License

SPDX-FileCopyrightText: 2025 Olaf Kfreund
SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
