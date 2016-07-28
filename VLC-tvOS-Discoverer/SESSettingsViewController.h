//
//  SESSettingsViewController.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 12/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <UIKit/UIKit.h>

extern NSString *const SESSettingsMajorPlaybackConfigurationChange;

@interface SESSettingsViewController : UIViewController

@property (readwrite, weak, nonatomic) IBOutlet UITableView *channelListTableView;
@property (readwrite, weak, nonatomic) IBOutlet UITableView *satelliteListTableView;
@property (readwrite, weak, nonatomic) IBOutlet UITableView *serverListTableView;

@property (readwrite, weak, nonatomic) IBOutlet UIButton *addChannelListButton;
@property (readwrite, weak, nonatomic) IBOutlet UIButton *editChannelListButton;
@property (readwrite, weak, nonatomic) IBOutlet UIButton *addServerButton;
@property (readwrite, weak, nonatomic) IBOutlet UIButton *editServerButton;

- (IBAction)addServer:(id)sender;
- (IBAction)addChannelList:(id)sender;

- (IBAction)editAction:(id)sender;

@end
