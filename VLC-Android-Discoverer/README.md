Base Android app for SatIp live streaming using VLC
=========================================================

Build application
-----------

This project is shipped with a prebuild ARMv7 LibVLC aar module.

So, you just have to run gradle build (like with the 'Run' button from Android Studio) to build it.

Build LibVLC
------------

You can build your own version of LibVLC, especially to address other abis than ARMv7  
Check https://wiki.videolan.org/AndroidCompile/  
You need a Linux OS to build VLC.

And pay attention to https://wiki.videolan.org/AndroidCompile/#Can.27t_run_aapt_or_adb_on_Linux_64-bit for building on a 64 bits distribution

LibVLC aar file will be placed in libvlc/build/outputs/aar/ folder
