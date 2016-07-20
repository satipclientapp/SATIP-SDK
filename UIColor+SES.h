//
//  UIColor+SES.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 14/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UIColor (SESColors)

+ (UIColor *)sesPureWhite;
+ (UIColor *)sesCloudColor;
+ (UIColor *)sesLightGrayColor;
+ (UIColor *)sesButtonLabelBlueColor;
+ (UIColor *)sesGrayColor;
+ (UIColor *)sesPearlColor;
+ (UIImage *)sesImageWithColor:(UIColor *)color;

@end
