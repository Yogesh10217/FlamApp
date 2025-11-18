# Flamapp - Real-Time Edge Detection Viewer

Android app with OpenCV C++ edge detection processing via JNI, rendered using OpenGL ES 2.0.

## ✅ Features Implemented

### Android Application
- ✅ Camera2 API integration for frame capture (640x480 @ ~15-30 FPS)
- ✅ Real-time Canny edge detection in C++ using OpenCV
- ✅ OpenGL ES 2.0 rendering with texture updates
- ✅ JNI/NDK integration for native processing
- ✅ FPS counter and processing time display
- ✅ Toggle between raw/processed views (button)
- ✅ Proper permission handling
- ✅ Background thread processing

### Web Viewer
- ✅ TypeScript-based static viewer
- ✅ Base64 image display
- ✅ FPS and resolution stats overlay

## 📷 Screenshots

![Flamapp Output Screenshot](Output%20Screen%20Shot.png)

*Screenshot showing the real-time edge detection output on Android device*

## 📁 Project Structure

```
Flamapp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/flamapp/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── GLRenderer.kt
│   │   │   │   └── FullScreenQuad.kt
│   │   │   ├── cpp/
│   │   │   │   ├── native-lib.cpp
│   │   │   │   └── CMakeLists.txt
│   │   │   ├── res/
│   │   │   │   └── layout/
│   │   │   │       └── activity_main.xml
│   │   │   └── AndroidManifest.xml
│   │   │   └── build.gradle
├── external-libs/
│   └── OpenCV-android-sdk/
│       └── sdk/
│           └── native/
│               ├── jni/include/
│               └── libs/
│                   ├── arm64-v8a/
│                   └── armeabi-v7a/
├── web/
│   ├── index.html
│   ├── index.ts
│   ├── package.json
│   └── tsconfig.json
├── build.gradle
└── README.md
```

## ⚙ Setup Instructions

### Prerequisites
1. **Android Studio** (Flamingo or later)
2. **Android NDK** (version 25.1.8937393 or compatible)
3. **OpenCV Android SDK** (version 4.x)
4. **Node.js and npm** (for TypeScript web viewer)

### Step 1: Download OpenCV Android SDK

1. Download OpenCV Android SDK from: https://opencv.org/releases/
2. Extract the downloaded zip file
3. Create `external-libs` directory in your project root
4. Copy the extracted SDK to: `Flamapp/external-libs/OpenCV-android-sdk/`

Your directory structure should look like:
```
Flamapp/
├── app/
├── external-libs/
│   └── OpenCV-android-sdk/
│       └── sdk/
│           └── native/
│               ├── jni/include/  (OpenCV headers)
│               └── libs/          (prebuilt .so files)
│                   ├── arm64-v8a/libopencv_java4.so
│                   └── armeabi-v7a/libopencv_java4.so
```

### Step 2: File Placement

Place each file in the correct location:

**Android Files:**
- `MainActivity.kt` → `app/src/main/java/com/example/flamapp/MainActivity.kt`
- `GLRenderer.kt` → `app/src/main/java/com/example/flamapp/GLRenderer.kt`
- `FullScreenQuad.kt` → `app/src/main/java/com/example/flamapp/FullScreenQuad.kt`
- `native-lib.cpp` → `app/src/main/cpp/native-lib.cpp`
- `CMakeLists.txt` → `app/src/main/cpp/CMakeLists.txt`
- `activity_main.xml` → `app/src/main/res/layout/activity_main.xml`
- `AndroidManifest.xml` → `app/src/main/AndroidManifest.xml`
- `build.gradle` → `app/build.gradle`

**Web Files:**
- `index.html` → `web/index.html`
- `index.ts` → `web/index.ts`
- `package.json` → `web/package.json`
- `tsconfig.json` → `web/tsconfig.json`

### Step 3: Configure Project

1. Open the project in Android Studio
2. Sync Gradle: **File → Sync Project with Gradle Files**
3. Ensure NDK is installed: **Tools → SDK Manager → SDK Tools → NDK (Side by side)**
4. Update `local.properties` (created automatically by Android Studio):
   ```properties
   sdk.dir=/path/to/Android/sdk
   ndk.dir=/path/to/Android/sdk/ndk/25.1.8937393
   ```

### Step 4: Build Android App

1. **Clean Project**: Build → Clean Project
2. **Rebuild Project**: Build → Rebuild Project
3. Connect an Android device (API 24+) or start an emulator
4. **Run**: Run → Run 'app' (or press Shift + F10)
5. Grant camera permission when prompted

### Step 5: Setup Web Viewer

```bash
cd web
npm install
npm run build
```

Open `web/index.html` in a browser to see the static demo.

## 🔧 Troubleshooting

### App Crashes on Launch

**1. Check Logcat for errors:**
```bash
adb logcat | grep -E "MainActivity|FlamappNative|OpenGL|AndroidRuntime"
```
#### OpenGL Context Error
- **Check device support**: Ensure device supports OpenGL ES 2.0
- **Emulator settings**: Use a system image with Google Play (has better OpenGL support)


### Build Errors

**NDK version mismatch:**
```bash
# Find installed NDK version
ls $ANDROID_SDK_ROOT/ndk/

# Update in app/build.gradle
android {
    ndkVersion "YOUR_INSTALLED_VERSION"
}
```

**Gradle sync failed:**
- Update Gradle plugin in project-level build.gradle
- File → Invalidate Caches / Restart

### Performance Issues

If FPS is low (<10 FPS):

1. **Reduce resolution** in MainActivity.kt:
   ```kotlin
   val targetSize = Size(320, 240)  // Instead of 640x480
   ```

2. **Simplify processing** in native-lib.cpp:
   ```cpp
   // Reduce Gaussian blur kernel
   cv::GaussianBlur(grayMat, blurred, cv::Size(3, 3), 1.0);
   ```

3. **Check ABI**: Use `arm64-v8a` for 64-bit devices (faster)
   ```gradle
   ndk {
       abiFilters 'arm64-v8a'  // Remove armeabi-v7a for testing
   }
   ```

### Web Viewer Issues

**TypeScript compilation errors:**
```bash
cd web
npm install -g typescript
tsc --version  # Should be 4.9+
npm run build
```

## 🧠 Architecture

### Quick Overview

Flamapp demonstrates a sophisticated cross-platform architecture combining Android native development with web technologies:

#### JNI & Frame Flow
- **JNI Bridge**: Kotlin (MainActivity.kt) calls native C++ functions via JNI for high-performance image processing
- **Frame Processing Pipeline**:
  ```
  Camera2 API (YUV_420_888) → NV21 Conversion → JNI → native-lib.cpp (OpenCV Canny) → PNG Output → OpenGL ES 2.0 Rendering
  ```
- **Native Processing**: OpenCV handles color space conversions, Gaussian blur, and Canny edge detection in C++ for optimal performance

#### TypeScript Web Component
- **Static Viewer**: TypeScript-based web interface for displaying processed frames
- **Data Handling**: Base64-encoded images with real-time stats overlay (FPS, resolution, processing time)
- **Integration Ready**: Designed for future WebSocket/HTTP integration with Android app

### Key Technical Components

1. **native-lib.cpp**: C++ OpenCV processing (NV21→RGBA→Grayscale→Blur→Canny→PNG encoding)
2. **MainActivity.kt**: Camera management, JNI calls, UI controls, background threading
3. **GLRenderer.kt**: OpenGL ES 2.0 texture rendering and shader management
4. **index.ts**: TypeScript utilities for image display and stats visualization

## 📧 Contact

For questions about this assessment submission:
- GitHub: [https://github.com/Yogesh10217]
- Email: [eyogesh104@gmail.com]

---

