//
//  AppDelegate.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 16/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "AppDelegate.h"
#import "SESChannelListAndPlayViewController.h"
#import "SESSettingsViewController.h"
#import "UIColor+SES.h"
#import "SESServerDiscoveryController.h"

@interface AppDelegate ()
{
    /* store the root view controller for potential future reference */
    SESChannelListAndPlayViewController *_playbackVC;
    SESSettingsViewController *_settingsVC;
}
@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    /* quick traditional app launch without storyboards */
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen
                                                    ] bounds]];

    /* load platform specific UI */
#if TARGET_OS_TV
    _playbackVC = [[SESChannelListAndPlayViewController alloc] initWithNibName:nil bundle:nil];
    _settingsVC = [[SESSettingsViewController alloc] initWithNibName:nil bundle:nil];
#else
    _playbackVC = [[SESChannelListAndPlayViewController alloc] initWithNibName:@"SESChannelListAndPlayViewController-iPad" bundle:nil];
    _settingsVC = [[SESSettingsViewController alloc] initWithNibName:@"SESSettingsViewController-ipad" bundle:nil];
#endif

    UITabBarController *tabBarController = [[UITabBarController alloc] init];
    tabBarController.tabBar.barTintColor = [UIColor whiteColor];
    UINavigationController *playbackNavCon = [[UINavigationController alloc] initWithRootViewController:_playbackVC];
    [playbackNavCon.tabBarItem setTitle:@"Live TV"];
#if TARGET_OS_TV
    [playbackNavCon.tabBarItem setTitleTextAttributes:@{NSForegroundColorAttributeName : [UIColor sesButtonLabelBlueColor]} forState:UIControlStateHighlighted];
    [playbackNavCon.tabBarItem setTitleTextAttributes:@{NSForegroundColorAttributeName : [UIColor lightGrayColor]} forState:UIControlStateNormal];
#else
    playbackNavCon.tabBarItem.image = [UIImage imageNamed:@"playbackIcon"];
    playbackNavCon.navigationBarHidden = YES;
#endif
    UINavigationController *settingsNavCon = [[UINavigationController alloc] initWithRootViewController:_settingsVC];
#if TARGET_OS_TV
    [settingsNavCon.tabBarItem setTitleTextAttributes:@{NSForegroundColorAttributeName : [UIColor sesButtonLabelBlueColor]} forState:UIControlStateHighlighted];
    [settingsNavCon.tabBarItem setTitleTextAttributes:@{NSForegroundColorAttributeName : [UIColor lightGrayColor]} forState:UIControlStateNormal];
#else
    settingsNavCon.tabBarItem.image = [UIImage imageNamed:@"settingsIcon"];
    settingsNavCon.navigationBarHidden = YES;
#endif
    tabBarController.viewControllers = @[playbackNavCon, settingsNavCon];

    UIImageView *logoView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"logo"]];
    CGRect logoViewFrame = logoView.frame;
    CGRect tabBarFrame = tabBarController.tabBar.frame;
#if TARGET_OS_TV
    logoViewFrame.origin.y = (tabBarFrame.size.height - logoViewFrame.size.height) / 2.;
    logoViewFrame.origin.x = 40.;
#else
    logoViewFrame.size.width = logoViewFrame.size.width / 2.;
    logoViewFrame.size.height = logoViewFrame.size.height / 2.;
    logoViewFrame.origin.y = (tabBarFrame.size.height - logoViewFrame.size.height) / 2.;
    logoViewFrame.origin.x = 20.;
#endif
    logoView.frame = logoViewFrame;
    [tabBarController.tabBar addSubview:logoView];

    self.window.rootViewController = tabBarController;

    [self.window makeKeyAndVisible];

    /* do the expensive call of starting the discovery - should be done once only */
    SESServerDiscoveryController *discoveryController = [SESServerDiscoveryController sharedDiscoveryController];
    [discoveryController startDiscovery];
    tabBarController.selectedIndex = discoveryController.selectedServerIndex == -1 ? 1 : 0;

    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    [[SESServerDiscoveryController sharedDiscoveryController] stopDiscovery];
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

@end
