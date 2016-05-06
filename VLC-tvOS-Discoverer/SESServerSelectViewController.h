//
//  SESServerSelectViewController.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface SESServerSelectViewController : UIViewController

/* the table to show the servers in */
@property (readwrite, weak) IBOutlet UITableView *serverTableView;
@property (readwrite, weak) IBOutlet UILabel *viewControllerTitleLabel;

@end
