cmake_minimum_required(VERSION 3.10.2)
project("flamapp")

# Set OpenCV paths
set(OPENCV_SDK_ROOT "${CMAKE_SOURCE_DIR}/../../../../external-libs/OpenCV-android-sdk")
set(OPENCV_INCLUDE_DIR "${OPENCV_SDK_ROOT}/sdk/native/jni/include")

if(NOT ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI not set")
endif()

set(OPENCV_LIB_PATH "${OPENCV_SDK_ROOT}/sdk/native/libs/${ANDROID_ABI}")

# Verify OpenCV exists
if(NOT EXISTS ${OPENCV_INCLUDE_DIR})
    message(FATAL_ERROR "OpenCV include directory not found at: ${OPENCV_INCLUDE_DIR}")
endif()

if(NOT EXISTS ${OPENCV_LIB_PATH})
    message(FATAL_ERROR "OpenCV libs not found at: ${OPENCV_LIB_PATH}")
endif()

message(STATUS "✓ OpenCV include: ${OPENCV_INCLUDE_DIR}")
message(STATUS "✓ OpenCV lib path: ${OPENCV_LIB_PATH}")

# Create main library
add_library(flamapp SHARED native-lib.cpp)

# Include OpenCV headers
include_directories(${OPENCV_INCLUDE_DIR})

# Import OpenCV shared library
set(OPENCV_SO "${OPENCV_LIB_PATH}/libopencv_java4.so")
if(NOT EXISTS ${OPENCV_SO})
    message(FATAL_ERROR "libopencv_java4.so not found at: ${OPENCV_SO}")
endif()

add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION "${OPENCV_SO}")
message(STATUS "✓ Using OpenCV: ${OPENCV_SO}")

# Find system libraries
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
# Link libraries (updated to include c++_shared)
target_link_libraries(
        flamapp
        PRIVATE
        opencv_java4
        ${log-lib}
        ${android-lib}
        # Link the shared libc++ runtime so standard C++ symbols are available at runtime.
        c++_shared
)
