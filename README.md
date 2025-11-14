# Real-Time Edge Detection Viewer

This Android app captures camera frames, processes them using OpenCV in C++ via JNI, and displays the output using OpenGL ES. It also includes a TypeScript-based web viewer for displaying processed frames.

## Features Implemented

### Android App
- Camera feed integration using Camera2 API with ImageReader for frame capture.
- Frame processing via OpenCV C++ (Canny Edge Detection).
- Render output with OpenGL ES 2.0 as a texture.
- Toggle button to switch between raw camera feed (TextureView) and processed output (GLSurfaceView).
- FPS counter displaying processing time.

### Web Viewer
- Minimal TypeScript web page displaying a sample processed frame (base64 encoded image).
- Basic text overlay for frame stats (FPS, resolution).

## Setup Instructions

### Prerequisites
- Android Studio with NDK support.
- OpenCV Android SDK (included in `external-libs/OpenCV-android-sdk`).
- Node.js for web viewer.

### Android App
1. Open the project in Android Studio.
2. Ensure NDK is installed and configured.
3. Build and run on a device with camera permissions.

### OpenCV Dependencies
- OpenCV is included as a prebuilt library in `external-libs/OpenCV-android-sdk`.
- CMakeLists.txt is configured to find and link OpenCV.

### Web Viewer
1. Navigate to `web/` directory.
2. Run `npm install`.
3. Run `npm run build` to compile TypeScript.
4. Run `npm start` to start the live server.

## Architecture Explanation

### JNI Frame Flow
1. Camera frames are captured via ImageReader in YUV format.
2. Frames are converted to NV21 and sent to native C++ via JNI.
3. OpenCV processes the frame (Canny edge detection or grayscale).
4. Processed RGBA bytes are returned to Java.
5. GLRenderer updates the OpenGL texture and renders it.

### TypeScript Part
- `index.ts` sets the image source to a base64 encoded processed frame and updates stats.
- HTML provides a simple UI for displaying the image and stats.

## Screenshots/GIF
(Add screenshots or GIF of the app in action here.)

## Project Structure
- `/app`: Kotlin/Java code for Android app.
- `/jni`: C++ OpenCV processing (native-lib.cpp).
- `/gl`: OpenGL renderer classes (GLRenderer.kt, FullScreenQuad.kt).
- `/web`: TypeScript web viewer.
