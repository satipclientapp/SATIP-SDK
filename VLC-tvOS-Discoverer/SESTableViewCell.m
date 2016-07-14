//
//  SESTableViewCell.m
//  VLC-Discoverer
//
//  Created by videolan on 03/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESTableViewCell.h"
#import "UIColor+SES.h"

@implementation SESTableViewCell

- (void)awakeFromNib {
    [super awakeFromNib];

    self.backgroundColor = [UIColor sesPureWhite];

#if TARGET_OS_TV
    self.channelNameLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:29.];
#else
    self.channelNameLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:21.];
#endif
}

#if TARGET_OS_TV
- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    if (selected) {
        self.backgroundGradientImageView.hidden = NO;
    } else {
        self.backgroundGradientImageView.hidden = YES;
    }
}
#endif

@end
