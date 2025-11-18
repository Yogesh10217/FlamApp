cmake_minimum_required(VERSION 3.22.1)
project("flamapp")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# OpenCV paths
set(OPENCV_SDK_ROOT "${CMAKE_SOURCE_DIR}/../../../../external-libs/OpenCV-android-sdk")
set(OPENCV_INCLUDE_DIR "${OPENCV_SDK_ROOT}/sdk/native/jni/include")

if(NOT ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI not set!")
endif()

set(OPENCV_LIB_PATH "${OPENCV_SDK_ROOT}/sdk/native/libs/${ANDROID_ABI}")

# Verify OpenCV paths exist
if(NOT EXISTS ${OPENCV_INCLUDE_DIR})
    message(FATAL_ERROR "OpenCV headers not found at: ${OPENCV_INCLUDE_DIR}")
endif()

if(NOT EXISTS ${OPENCV_LIB_PATH})
    message(FATAL_ERROR "OpenCV libs not found at: ${OPENCV_LIB_PATH}")
endif()

message(STATUS "✓ OpenCV include: ${OPENCV_INCLUDE_DIR}")
message(STATUS "✓ OpenCV lib path: ${OPENCV_LIB_PATH}")

# Create main library
add_library(flamapp SHARED native-lib.cpp)

# Include OpenCV headers
target_include_directories(flamapp PRIVATE ${OPENCV_INCLUDE_DIR})

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

# Find C++ shared library (THIS IS CRITICAL!)
find_library(cpp-lib c++_shared)

if(NOT log-lib)
    message(FATAL_ERROR "log library not found")
endif()

if(NOT cpp-lib)
    message(WARNING "c++_shared not found via find_library, will link manually")
    # Manually set path to libc++_shared.so
    set(cpp-lib "${ANDROID_NDK}/sources/cxx-stl/llvm-libc++/libs/${ANDROID_ABI}/libc++_shared.so")
endif()

# Link libraries - ORDER MATTERS!
target_link_libraries(flamapp
        opencv_java4
        ${cpp-lib}        # C++ standard library (MUST BE INCLUDED!)
        ${log-lib}
        ${android-lib}
        -latomic
        -lm
)

# Copy OpenCV .so to jniLibs directory for APK packaging
set(JNILIBS_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
add_custom_command(TARGET flamapp POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${JNILIBS_DIR}
        COMMAND ${CMAKE_COMMAND} -E copy_if_different ${OPENCV_SO} ${JNILIBS_DIR}/
        COMMENT "Copying OpenCV library to jniLibs/${ANDROID_ABI}/"
)

message(STATUS "✓ Will copy OpenCV to: ${JNILIBS_DIR}")
message(STATUS "✓ CMake configuration complete")