//
//  SESTableViewCell.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 06/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface SESTableViewCell : UITableViewCell

@property (readwrite, strong, nonatomic) IBOutlet UIImageView *channelIconImageView;
@property (readwrite, strong, nonatomic) IBOutlet UILabel *channelNameLabel;
@property (readwrite, strong, nonatomic) IBOutlet UIImageView *backgroundGradientImageView;

@end
