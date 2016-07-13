//
//  SESColors.m
//  VLC-Discoverer
//
//  Created by videolan on 03/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SESColors.h"

@implementation SESColors

+ (UIColor*)SESPureWhite
{
    return [UIColor whiteColor];
}

+ (UIColor*)SESCloudColor
{
    return [UIColor colorWithRed:0.66 green:0.8 blue:0.93 alpha:1];
}

+ (UIColor*)SESLightGrayColor
{
    return [UIColor colorWithRed:0.97 green:0.97 blue:0.97 alpha:1];
}

+ (UIColor*)SESGrayColor
{
    return [UIColor colorWithRed:0.96 green:0.96 blue:0.96 alpha:1];
}

+ (UIColor*)SESPearlColor
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
