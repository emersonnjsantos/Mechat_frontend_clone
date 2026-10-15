cmake_minimum_required(VERSION 3.22.1)

project("whatsapp_native")

add_library(
    whatsapp_native
    SHARED
    native_bridge.cpp
    crypto/curve25519.c
    codec/opus_helper.c
)

target_include_directories(
    whatsapp_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/crypto
    ${CMAKE_CURRENT_SOURCE_DIR}/codec
)

find_library(
    log-lib
    log
)

target_link_libraries(
    whatsapp_native
    ${log-lib}
    m
)
