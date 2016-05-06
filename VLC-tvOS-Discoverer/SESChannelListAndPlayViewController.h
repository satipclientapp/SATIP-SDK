//
//  SESChannelListAndPlayViewController.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <UIKit/UIKit.h>

@class VLCMedia;

@interface SESChannelListAndPlayViewController : UIViewController

/* the server the VC is supposed to interact with and show its contents
 * this is set by the server discovery VC before this VC is pushed */
@property (readwrite, nonatomic, strong) VLCMedia *serverMediaItem;

/* a handful of UI elements */
@property (readwrite, weak, nonatomic) IBOutlet UITableView *channelListTableView;
@property (readwrite, weak, nonatomic) IBOutlet UIView *videoOutputView;
@property (readwrite, weak, nonatomic) IBOutlet UIButton *fullscreenButton;

/* action for the fullscreen clicker */
- (IBAction)fullscreenAction:(id)sender;

@end
