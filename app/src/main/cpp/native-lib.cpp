#include <jni.h>
#include <vector>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_flamapp_MainActivity_processFrameNV21(JNIEnv *env, jobject thiz,
                                                       jbyteArray input_, jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    int ySize = width * height;
    int uvSize = width * height / 2;
    // Create Mat from NV21 buffer
    Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*) input);
    Mat rgba;
    cvtColor(yuv, rgba, COLOR_YUV2RGBA_NV21);

    // Apply Canny Edge Detection
    Mat gray, edges;
    cvtColor(rgba, gray, COLOR_RGBA2GRAY);
    GaussianBlur(gray, gray, Size(5,5), 1.5);
    Canny(gray, edges, 50, 150);

    Mat outRGBA;
    cvtColor(edges, outRGBA, COLOR_GRAY2RGBA);

    // Return bytes
    int outSize = outRGBA.total() * outRGBA.elemSize();
    jbyteArray outArr = env->NewByteArray(outSize);
    env->SetByteArrayRegion(outArr, 0, outSize, reinterpret_cast<const jbyte*>(outRGBA.data));
    env->ReleaseByteArrayElements(input_, input, JNI_ABORT);
    return outArr;
}
