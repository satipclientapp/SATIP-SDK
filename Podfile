# Uncomment this line to define a global platform for your project
# platform :ios, '8.0'
# Uncomment this line if you're using Swift
# use_frameworks!

source 'https://gitlab.com/videolabs/SAT-IP-Pods.git'
source 'https://github.com/CocoaPods/Specs.git'

use_frameworks!

target 'VLC-iOS-Discoverer' do
platform :ios, '9.2'

pod 'AFNetworking'
pod 'SAT-IP-DynamicVLCKit', '3.0.0a12'

end
post_install do |installer|
  installer.pods_project.targets.each do |target|
    if target.name == "AFNetworking-iOS" then
      target.build_configurations.each do |config|
        config.build_settings['ENABLE_BITCODE'] = 'NO'
      end
    end
  end
end

target 'VLC-tvOS-Discoverer' do
platform :tvos, '9.1'

pod 'AFNetworking'
pod 'SAT-IP-DynamicTVVLCKit', '3.0.0a12'

end
