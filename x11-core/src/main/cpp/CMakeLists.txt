cmake_minimum_required(VERSION 3.22.1)

project("x11core")

# Define header inclusion
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# Define the shared library target
add_library(
        x11core
        SHARED
        x11_bridge_jni.cpp
)

# Locate Android standard libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)

# Link the libraries
target_link_libraries(
        x11core
        ${log-lib}
        ${android-lib}
        ${jnigraphics-lib}
)
