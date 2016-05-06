//
//  SESTableViewCell.m
//  VLC-Discoverer
//
//  Created by videolan on 03/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESTableViewCell.h"
#import "SESColors.h"

@implementation SESTableViewCell

- (void)awakeFromNib {
    [super awakeFromNib];

    self.backgroundColor = [SESColors SESPureWhite];

#if TARGET_OS_TV
    self.channelNameLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:29.];
#else
    self.channelNameLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:21.];
#endif
}

- (void)didUpdateFocusInContext:(UIFocusUpdateContext *)context withAnimationCoordinator:(UIFocusAnimationCoordinator *)coordinator
{
    [super didUpdateFocusInContext:context withAnimationCoordinator:coordinator];
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];
    UITableViewCell *cell = self;
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    if (selected) {
        CAGradientLayer *gradient = [CAGradientLayer layer];
        gradient.frame = cell.bounds;
        gradient.colors = [NSArray arrayWithObjects:(id)[[SESColors SESPureWhite]CGColor], (id)[[SESColors SESCloudColor]CGColor], nil];
        [gradient setStartPoint:CGPointMake(1, 1)];
        [gradient setEndPoint:CGPointMake(0, 0)];
        [cell.layer insertSublayer:gradient atIndex:0];
    } else {
        while ([[cell.layer.sublayers objectAtIndex:0] isKindOfClass:[CAGradientLayer class]])
            [[cell.layer.sublayers objectAtIndex:0] removeFromSuperlayer];
    }
}

@end
