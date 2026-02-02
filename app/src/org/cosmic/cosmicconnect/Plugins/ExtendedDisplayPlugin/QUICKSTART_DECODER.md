# Quick Start: H.264 Video Decoder

## 5-Minute Integration Guide

### Step 1: Create Surface
```kotlin
// In your Activity or Fragment
val surfaceView = SurfaceView(context)
setContentView(surfaceView)
```

### Step 2: Initialize Decoder
```kotlin
class VideoStreamActivity : AppCompatActivity() {
    private var decoder: VideoDecoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startDecoder(holder.surface)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopDecoder()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        })
    }

    private fun startDecoder(surface: Surface) {
        // Configure for 1080p streaming
        val config = DecoderConfig.create1080p()

        decoder = VideoDecoder(config, surface).apply {
            setEventCallback(object : VideoDecoder.EventCallback {
                override fun onFrameRendered(presentationTimeUs: Long) {
                    // Frame displayed successfully
                }

                override fun onError(error: Exception) {
                    Log.e(TAG, "Decoder error", error)
                    // Handle error (show UI message, reconnect, etc.)
                }
            })

            start()
        }
    }

    private fun stopDecoder() {
        decoder?.stop()
        decoder = null
    }
}
```

### Step 3: Feed Encoded Frames
```kotlin
// When H.264 encoded frame arrives (e.g., from WebRTC)
fun onEncodedFrameReceived(data: ByteArray, isKeyFrame: Boolean) {
    val frame = VideoFrame(
        data = data,
        presentationTimeUs = System.nanoTime() / 1000,
        isKeyFrame = isKeyFrame
    )

    decoder?.decodeFrame(frame)
}
```

### Step 4: Monitor Performance (Optional)
```kotlin
// Get statistics periodically
val stats = decoder?.getStatistics()
Log.d(TAG, "Decoded: ${stats?.framesDecoded}, Dropped: ${stats?.framesDropped}")
Log.d(TAG, "Average latency: ${stats?.averageLatencyMs}ms")
```

## Common Patterns

### Pattern 1: Resolution Change
```kotlin
fun changeResolution(width: Int, height: Int, surface: Surface) {
    // Stop current decoder
    decoder?.stop()

    // Create new decoder with new resolution
    val config = DecoderConfig(width, height)
    decoder = VideoDecoder(config, surface)
    decoder?.start()
}
```

### Pattern 2: Pause/Resume
```kotlin
override fun onPause() {
    super.onPause()
    decoder?.stop()  // Release resources when paused
}

override fun onResume() {
    super.onResume()
    // Restart decoder with saved configuration
    decoder?.start()
}
```

### Pattern 3: Error Recovery
```kotlin
setEventCallback(object : VideoDecoder.EventCallback {
    override fun onError(error: Exception) {
        if (error is MediaCodec.CodecException) {
            if (error.isRecoverable) {
                // Try restarting decoder
                decoder?.stop()
                decoder?.start()
            } else {
                // Fatal error - notify user
                showErrorDialog("Video decoder failed")
            }
        }
    }
})
```

### Pattern 4: Check Buffer Availability
```kotlin
fun smartEncode(frame: VideoFrame) {
    if (decoder?.hasAvailableInputBuffers() == true) {
        decoder?.decodeFrame(frame)
    } else {
        // Buffer full - drop frame or implement backpressure
        Log.w(TAG, "Dropping frame - decoder busy")
    }
}
```

## Configuration Examples

### 720p Low Latency (Most Compatible)
```kotlin
val config = DecoderConfig.create720p(lowLatency = true)
```

### 1080p Standard (Recommended)
```kotlin
val config = DecoderConfig.create1080p(lowLatency = true)
```

### 4K High Quality (High-End Devices)
```kotlin
val config = DecoderConfig.create4K(lowLatency = true)
```

### Custom Configuration
```kotlin
val config = DecoderConfig(
    width = 1280,
    height = 720,
    lowLatencyMode = true,
    frameRate = 60,  // 60fps
    maxInputSize = 512 * 1024  // 512KB buffer
)
```

## Troubleshooting

### "Decoder not running" errors
**Solution:** Call `decoder.start()` before `decodeFrame()`

### Frames not displaying
**Check:**
1. Surface is valid and attached to view
2. Surface lifecycle (created before decoder start)
3. H.264 data is valid (starts with 0x00 0x00 0x00 0x01)
4. First frame is a keyframe (isKeyFrame = true)

### High latency (>100ms)
**Try:**
1. Verify `lowLatencyMode = true` in config
2. Reduce resolution (1080p → 720p)
3. Check network latency separately
4. Verify encoder is using low-latency preset

### Decoder crashes
**Common causes:**
1. Invalid H.264 data (corrupt frame)
2. Unsupported resolution
3. Missing SPS/PPS headers
4. Surface destroyed before decoder stopped

**Solution:** Add try-catch and log errors:
```kotlin
try {
    decoder?.decodeFrame(frame)
} catch (e: Exception) {
    Log.e(TAG, "Decode failed", e)
}
```

## Performance Tips

1. **Use Hardware Decoder** - Enabled by default, don't override
2. **Low-Latency Mode** - Always enable for real-time streaming
3. **Keyframe Frequency** - Send keyframe every 1-2 seconds
4. **Resolution** - Start with 720p, increase if performance allows
5. **Monitor Statistics** - Track dropped frames, optimize if >5%

## Integration Checklist

- [ ] Surface created and valid before decoder start
- [ ] First frame is a keyframe
- [ ] Presentation timestamps are monotonically increasing
- [ ] Error callback implemented and tested
- [ ] Decoder stopped when Activity destroyed
- [ ] Performance statistics monitored
- [ ] Tested on target devices (various SoCs)

## See Also

- **Full Documentation:** `README_VIDEO_DECODER.md`
- **Implementation Details:** `IMPLEMENTATION_SUMMARY_ISSUE_138.md`
- **Android MediaCodec:** https://developer.android.com/reference/android/media/MediaCodec
