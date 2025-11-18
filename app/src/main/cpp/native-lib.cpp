#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>

#define TAG "FlamappNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_flamapp_MainActivity_processFrameNV21(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray input,
        jint width,
        jint height) {

    LOGD("=== processFrameNV21 START: %dx%d ===", width, height);

    try {
        // Get input bytes
        jbyte* inputBytes = env->GetByteArrayElements(input, nullptr);
        if (!inputBytes) {
            LOGE("✗ Failed to get input bytes");
            return nullptr;
        }
        LOGV("✓ Got input bytes");

        // Calculate expected size
        int expectedSize = width * height * 3 / 2; // NV21 format
        jsize actualSize = env->GetArrayLength(input);
        LOGD("Input size: expected=%d, actual=%d", expectedSize, actualSize);

        // Convert NV21 to Mat
        cv::Mat yuvMat(height + height / 2, width, CV_8UC1, (unsigned char*)inputBytes);
        cv::Mat rgbaMat(height, width, CV_8UC4);

        LOGV("✓ Created Mats");

        // Convert NV21 to RGBA
        cv::cvtColor(yuvMat, rgbaMat, cv::COLOR_YUV2RGBA_NV21);
        LOGD("✓ Converted NV21 to RGBA");

        // Release input array (no copy back needed)
        env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);

        // Convert to grayscale for edge detection
        cv::Mat grayMat;
        cv::cvtColor(rgbaMat, grayMat, cv::COLOR_RGBA2GRAY);
        LOGD("✓ Converted to grayscale");

        // Apply Gaussian blur to reduce noise
        cv::Mat blurred;
        cv::GaussianBlur(grayMat, blurred, cv::Size(5, 5), 1.5);
        LOGD("✓ Applied Gaussian blur");

        // Apply Canny edge detection
        cv::Mat edges;
        cv::Canny(blurred, edges, 50, 150);
        LOGD("✓ Applied Canny edge detection");

        // Convert back to RGBA for display
        cv::Mat edgesRgba;
        cv::cvtColor(edges, edgesRgba, cv::COLOR_GRAY2RGBA);
        LOGD("✓ Converted edges to RGBA");

        // Create output byte array
        int outputSize = edgesRgba.total() * edgesRgba.elemSize();
        jbyteArray output = env->NewByteArray(outputSize);
        if (!output) {
            LOGE("✗ Failed to create output array");
            return nullptr;
        }

        // Copy data to output
        env->SetByteArrayRegion(output, 0, outputSize,
                                reinterpret_cast<jbyte*>(edgesRgba.data));

        LOGD("✓✓✓ Frame processed successfully: %dx%d, output size: %d bytes ✓✓✓",
             width, height, outputSize);

        return output;

    } catch (const cv::Exception& e) {
        LOGE("✗✗✗ OpenCV exception: %s ✗✗✗", e.what());
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("✗✗✗ Standard exception: %s ✗✗✗", e.what());
        return nullptr;
    } catch (...) {
        LOGE("✗✗✗ Unknown exception in processFrameNV21 ✗✗✗");
        return nullptr;
    }
}