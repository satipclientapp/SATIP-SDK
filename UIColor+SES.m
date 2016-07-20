//
//  UIColor+SES.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 14/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "UIColor+SES.h"

@implementation UIColor (SESColors)

+ (UIColor*)sesPureWhite
{
    return [UIColor whiteColor];
}

+ (UIColor*)sesCloudColor
{
    return [UIColor colorWithRed:0.66 green:0.8 blue:0.93 alpha:1];
}

+ (UIColor*)sesLightGrayColor
{
    return [UIColor colorWithRed:0.97 green:0.97 blue:0.97 alpha:1];
}

+ (UIColor *)sesButtonLabelBlueColor
{
    return [UIColor colorWithRed:0.32 green:0.56 blue:0.82 alpha:1.];
}

+ (UIColor*)sesGrayColor
{
    return [UIColor colorWithRed:0.96 green:0.96 blue:0.96 alpha:1];
}

+ (UIColor*)sesPearlColor
{
    return [UIColor colorWithRed:0.93 green:0.93 blue:0.93 alpha:1];
}

+ (UIImage *)sesImageWithColor:(UIColor *)color
{
    CGRect rect = CGRectMake(0.0f, 0.0f, 1.0f, 1.0f);
    UIGraphicsBeginImageContext(rect.size);
    CGContextRef context = UIGraphicsGetCurrentContext();

    CGContextSetFillColorWithColor(context, [color CGColor]);
    CGContextFillRect(context, rect);

    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return image;
}

@end
