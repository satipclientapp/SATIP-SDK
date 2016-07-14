//
//  SESPlaybackController.h
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import <Foundation/Foundation.h>

@class VLCMedia;

/* basic delegation protocol so we can simplify device management event handling */
@protocol SESPlaybackControllerDelegate <NSObject>

@required
- (void)listOfServersWasUpdated;

@optional
- (void)discoveryFailed;

@end

/* a designated object to capsulize server discovery */
@interface SESServerDiscoveryController : NSObject

+ (instancetype)sharedDiscoveryController;

/* the delegate */
@property (readwrite, weak, nonatomic) NSObject<SESPlaybackControllerDelegate> *delegate;

/* start discovery of servers, note that this loads a VLCLibrary instance on each call and is therefore extremely expansive
 * should be called once only, even though this class is no strict singleton */
- (void)startDiscovery;

/* if you don't need updates about the existance of servers anymore, considering disabling the discovery
 * this frees the custom VLCLibrary instance among other things */
- (void)stopDiscovery;

/* how many servers did the object find so far? */
@property (readonly) NSInteger numberOfServers;

/* VLCMedia object of type VLCMediaTypeDirectory describing the actual server */
- (VLCMedia *)serverAtIndex:(NSInteger)index;

@property (readwrite, nonatomic) NSArray *customServers;
@property (readwrite, nonatomic) NSInteger selectedServerIndex;

@property (readwrite, nonatomic) NSArray *playlistTitlesToChooseFrom;
@property (readwrite, nonatomic) NSArray *playlistURLStringsToChooseFrom;
@property (readwrite, nonatomic) NSInteger selectedPlaylistIndex;

@property (readwrite, nonatomic) NSInteger lastPlayedChannelIndex;

@end
