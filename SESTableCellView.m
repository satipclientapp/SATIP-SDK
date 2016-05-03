//
//  SESTableCellView.m
//  VLC-Discoverer
//
//  Created by videolan on 03/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESTableCellView.h"
#import <Foundation/Foundation.h>

#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#import "SESColors.h"

@implementation SESTableCellView

- (void)setSelected:(BOOL)selected animated: (BOOL)animated
{
    [super setSelected:selected animated:animated];
    UITableViewCell *cell = self;
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    if (selected)
    {
        CAGradientLayer *gradient = [CAGradientLayer layer];
        gradient.frame = cell.bounds;
        gradient.colors = [NSArray arrayWithObjects:(id)[[UIColor whiteColor]CGColor], (id)[[SESColors SESCloudColor]CGColor], nil];
        [gradient setStartPoint:CGPointMake(1, 1)];
        [gradient setEndPoint:CGPointMake(0, 0)];
        [cell.layer insertSublayer:gradient atIndex:0];
    }
    else
    {
        while ([[cell.layer.sublayers objectAtIndex:0] isKindOfClass:[CAGradientLayer class]])
            [[cell.layer.sublayers objectAtIndex:0] removeFromSuperlayer];
    }
}

@end