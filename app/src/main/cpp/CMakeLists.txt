cmake_minimum_required(VERSION 3.10.2)
project("flamapp")

# Adjust path to your OpenCV Android SDK location (sdk/native/jni)
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../external-libs/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED core imgproc imgcodecs highgui)

add_library(flamapp SHARED native-lib.cpp)

include_directories(${OpenCV_INCLUDE_DIRS})
target_link_libraries(flamapp ${OpenCV_LIBS})
