cmake_minimum_required(VERSION 3.22)
project(rtcw C CXX)
add_compile_definitions(ARCH_STRING="aarch64")

get_filename_component(ROOT "${CMAKE_CURRENT_LIST_DIR}/../../.." ABSOLUTE)
set(RT ${ROOT}/engine/iortcw/SP/code)

# 16 KB page-size ELF segments (Android 15+ devices)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# SDL2 -> libSDL2.so
add_subdirectory(${ROOT}/engine/SDL ${CMAKE_BINARY_DIR}/SDL)

# gl4es without EGL: GL calls translated to the GLES context SDL creates -> libGL.so
set(APK_LIBDIR "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
set(NOEGL ON CACHE BOOL "" FORCE)
set(USE_ANDROID_LOG ON CACHE BOOL "" FORCE)
set(NO_INIT_CONSTRUCTOR ON CACHE BOOL "" FORCE)
add_subdirectory(${ROOT}/engine/gl4es ${CMAKE_BINARY_DIR}/gl4es)

# gl4es hardcodes output into the source tree and a libGL.so.1 name; Android packages only plain .so
set_target_properties(GL PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${APK_LIBDIR}" SUFFIX ".so" VERSION "" SOVERSION "")
file(REMOVE_RECURSE ${CMAKE_CURRENT_LIST_DIR}/lib)

file(GLOB JPEG_SRC ${RT}/jpeg-8c/j*.c)
list(FILTER JPEG_SRC EXCLUDE REGEX "jmem(ansi|name|mac|dos)|jpegtran|jcdjpeg|ckconfig|cjpeg|djpeg|rdjpgcom|wrjpgcom|jmemmgr_x")
file(GLOB ZLIB_SRC ${RT}/zlib-1.2.11/*.c)
set_source_files_properties(${JPEG_SRC} ${ZLIB_SRC} ${RT}/qcommon/puff.c PROPERTIES COMPILE_FLAGS -w)

file(GLOB BOTLIB ${RT}/botlib/*.c)
file(GLOB SERVER ${RT}/server/*.c)
file(GLOB RENDERER ${RT}/renderer/*.c)
list(FILTER RENDERER EXCLUDE REGEX "tr_altivec|tr_subs|tr_bloom")
file(GLOB QCOMMON ${RT}/qcommon/*.c)
list(FILTER QCOMMON EXCLUDE REGEX "vm_(x86|powerpc|sparc|armv7l|none)")
file(GLOB SPLINES ${RT}/splines/*.cpp)
list(FILTER SPLINES EXCLUDE REGEX "splines/q_shared")

set(CLIENT
    cl_cgame cl_cin cl_console cl_input cl_keys cl_main cl_net_chan cl_parse cl_scrn cl_touch cl_ui cl_avi
    snd_adpcm snd_dma snd_mem snd_mix snd_wavelet snd_main snd_codec snd_codec_wav)
list(TRANSFORM CLIENT PREPEND ${RT}/client/)
list(TRANSFORM CLIENT APPEND .c)

# SDL activity loads "main"
add_library(main SHARED
    ${QCOMMON} ${SERVER} ${BOTLIB} ${CLIENT} ${RENDERER} ${JPEG_SRC} ${ZLIB_SRC} ${SPLINES}
    ${RT}/sdl/sdl_input.c ${RT}/sdl/sdl_snd.c ${RT}/sdl/sdl_glimp.c ${RT}/sdl/sdl_gamma.c
    ${RT}/sys/con_log.c ${RT}/sys/sys_main.c ${RT}/sys/sys_unix.c ${RT}/sys/con_passive.c)

target_compile_definitions(main PRIVATE
    BOTLIB NO_GZIP NO_VM_COMPILED USE_GL4ES USE_INTERNAL_JPEG PRODUCT_VERSION="1.0-android" USE_ABS_MOUSE_GRAB_ON_ANDROID=0)
target_include_directories(main PRIVATE
    ${RT}/qcommon ${RT}/client ${RT}/server ${RT}/botlib ${RT}/renderer
    ${RT}/sdl ${RT}/sys ${RT}/jpeg-8c ${RT}/zlib-1.2.11 ${RT}/splines
    ${ROOT}/engine/SDL/include ${ROOT}/engine/gl4es/include)
target_link_libraries(main PRIVATE SDL2 GL log m dl)

# Game modules are native code (the original paks ship no QVMs). Loaded by name with dlopen,
# see Sys_LoadGameDll; they go into the APK's lib dir as libcgame.so / libqagame.so / libui.so.
function(game_module name defs)
    set(SRC ${ARGN})
    add_library(${name} SHARED ${SRC} ${RT}/qcommon/q_math.c ${RT}/qcommon/q_shared.c)
    target_compile_definitions(${name} PRIVATE ${defs})
    target_include_directories(${name} PRIVATE ${RT}/qcommon ${RT}/game ${RT}/cgame ${RT}/ui ${RT}/botlib)
    target_compile_options(${name} PRIVATE -w)
endfunction()

file(GLOB CG ${RT}/cgame/*.c)
game_module(cgame "CGAMEDLL;CGAME" ${CG} ${RT}/game/bg_animation.c ${RT}/game/bg_misc.c ${RT}/game/bg_pmove.c
    ${RT}/game/bg_slidemove.c ${RT}/game/bg_lib.c ${RT}/ui/ui_shared.c)
file(GLOB GM ${RT}/game/*.c)
game_module(qagame "GAMEDLL;QAGAME" ${GM})
game_module(ui "UI" ${RT}/ui/ui_main.c ${RT}/ui/ui_atoms.c ${RT}/ui/ui_gameinfo.c ${RT}/ui/ui_players.c
    ${RT}/ui/ui_shared.c ${RT}/ui/ui_syscalls.c ${RT}/game/bg_misc.c ${RT}/game/bg_lib.c)
