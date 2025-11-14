#include <jni.h>
#include <vector>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_flamapp_MainActivity_processFrameNV21(
        JNIEnv *env,
        jobject thiz,
        jbyteArray input_,
        jint width,
        jint height)
 {
    jbyte *input = env->GetByteArrayElements(input_, NULL);

    // Create Mat from NV21
    Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*) input);
    Mat rgba;
    cvtColor(yuv, rgba, COLOR_YUV2RGBA_NV21);

    // Canny Edge Processing
    Mat gray, edges;
    cvtColor(rgba, gray, COLOR_RGBA2GRAY);
    GaussianBlur(gray, gray, Size(5,5), 1.5);
    Canny(gray, edges, 50, 150);

    // Convert edges → RGBA to match PNG color requirement
    Mat outRGBA;
    cvtColor(edges, outRGBA, COLOR_GRAY2RGBA);

    // -------------------------------
    //     🔥 PNG Encoding Added
    // -------------------------------
    std::vector<uchar> buf;  // vector to hold PNG data
    cv::imencode(".png", outRGBA, buf);

    // Create Java byte[] to return
    jbyteArray outArr = env->NewByteArray(buf.size());
    env->SetByteArrayRegion(outArr, 0, buf.size(),
                            reinterpret_cast<const jbyte*>(buf.data()));

    // Release
    env->ReleaseByteArrayElements(input_, input, JNI_ABORT);

    return outArr;
}
