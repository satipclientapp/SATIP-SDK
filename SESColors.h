//
//  SESColors.h
//  VLC-Discoverer
//
//  Created by videolan on 03/05/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#ifndef SESColors_h
#define SESColors_h

#import <UIKit/UIKit.h>

@interface SESColors : NSObject

+ (UIColor*)SESPureWhite;
+ (UIColor*)SESCloudColor;
+ (UIColor*)SESLightGrayColor;
+ (UIColor*)SESGrayColor;
+ (UIColor*)SESPearlColor;
+ (UIImage *)sesImageWithColor:(UIColor *)color;

@end

#endif /* SESColors_h */
