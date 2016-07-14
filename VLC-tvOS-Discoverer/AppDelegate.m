//
//  AppDelegate.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 16/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "AppDelegate.h"
#import "SESServerSelectViewController.h"
#import "SESSettingsViewController.h"
#import "UIColor+SES.h"
#import "SESServerDiscoveryController.h"

@interface AppDelegate ()
{
    /* store the root view controller for potential future reference */
    SESServerSelectViewController *_serverVC;
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
    _serverVC = [[SESServerSelectViewController alloc] initWithNibName:nil bundle:nil];
    _settingsVC = [[SESSettingsViewController alloc] initWithNibName:nil bundle:nil];
#else
    _serverVC = [[SESServerSelectViewController alloc] initWithNibName:@"SESServerSelectViewController-iPad" bundle:nil];
    _settingsVC = [[SESSettingsViewController alloc] initWithNibName:@"SESSettingsViewController-ipad" bundle:nil];
#endif

    UITabBarController *tabBarController = [[UITabBarController alloc] init];
    tabBarController.tabBar.barTintColor = [UIColor sesCloudColor];
    tabBarController.viewControllers = @[_serverVC, _settingsVC];
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
    [[SESServerDiscoveryController sharedDiscoveryController] startDiscovery];

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
