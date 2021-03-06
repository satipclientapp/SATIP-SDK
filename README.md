# SAT>IP ShowCase app

This app demonstrates the use of VLCKit for iOS (dubbed _MobileVLCKit_) and tvOS (dubbed _TVVLCKit_) as well as libvlc for Android to discover SAT>IP servers, feed a custom channel list from a m3u file located on a remote server or the SAT>IP device and finally play a channel of your choice. Additionally, we give an introduction to video output view manipulation with VLCKit and libvlc with regard to live zooming and resizing.

On iOS and Android, libvlc makes use of the native hardware decoder exposed by the operating system to accelerate video rendering leading to a remarkably small CPU and memory footprint matching the native playback frameworks.

## Requirements

On Apple platform, this project uses the dynamic framework variants of VLCKit and therefore requires iOS 8 or tvOS 9. Note that bitcode is enabled only for the tvOS package. Static framework variants or bitcode for iOS require a compilation of VLCKit of your own. The sample project requires Xcode 7.3 or later. All code between the iOS and tvOS user interface is shared with only few platform specific code lines.

On Android, the sample project requires version 4.2 or later. It can be deployed both on Android and Android TV. For compilation, you need the latest version of Android Studio (currently v2.1.3) and the NDK (currently r12b).

## How to start for iOS and tvOS

 * Install cocoapods on your Mac. Note that the Mac-App is currently _not_ supported.
 * Execute "pod update" in your terminal within the project folder
 * Open the xcworkspace and execute the app either for iOS or tvOS

Note that you can switch from the "SAT-IP-VLCKit" pod to other VLCKit pods as soon as version 3.0 is released.

## How to start for Android

 * open the android project in Android Studio
 * Execute the app

Note that you can build your own versions of libvlc for Android from git and once version 3.0 is released, it will include support for SAT>IP natively.

## How to get help

See the [Android Compile] (https://wiki.videolan.org/AndroidCompile) and [VLCKit] (https://wiki.videolan.org/VLCKit) pages on the [VideoLAN] (http://www.videolan.org) website for details on compiling the needed library.

## Copyright of the sample code

The sample application code is Copyright (C) 2016 [SES S.A.] (http://www.ses.com) with contributions from [Sensory Minds GmbH] (http://www.sensoryminds.de) and [VideoLabs SAS] (https://videolabs.io) and distributed under the 3-clause BSD license. Therefore, you need to include the following paragraphs whenever you are using portions of the code or designs in your greater works and comply to the conditions.

    Copyright (C) 2016 SES S.A., SensoryMinds GmbH, VideoLabs SAS
    All rights reserved.

    Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Copyright of VLCKit and libvlc

Both libvlc and VLCKit are distributed under the Lesser GNU General Public License version 2.1 or later. Therefore, you need indicate the use of the libraries in the following suggested way and comply to the terms and conditions of the license, which you can [find here] (https://opensource.org/licenses/LGPL-2.1).

    libvlc
    Copyright (C) 1996-2016 VideoLAN and VLC Authors - LGPLv2.1 or later

    VLCKit
    Copyright (C) 2007-2016 Pierre d'Herbemont, Felix Paul Kühne, Faustino E. Osuna, et al. - LGPLv2.1 or later

    VideoLAN, VLC and VLC media player are internationally registered trademarks of the VideoLAN non-profit organization.
