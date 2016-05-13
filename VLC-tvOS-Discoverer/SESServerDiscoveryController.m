//
//  SESPlaybackController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESServerDiscoveryController.h"

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

/* we need to be a VLCMediaListDelegate, as this is how we get notified about servers becoming available or disappearing */
@interface SESServerDiscoveryController () <VLCMediaListDelegate>
{
    /* a handful of private instances */
    VLCMediaDiscoverer *_discoverer;
    VLCMediaList *_discoveredServerList;
    VLCLibrary *_discoveryLibrary;
}
@end

@implementation SESServerDiscoveryController

#pragma mark - setup and destruction

- (void)startDiscovery
{
    /* alloc our private library with the custom channel list URL - could be a local file in fact */
    _discoveryLibrary = [[VLCLibrary alloc] initWithOptions:@[@"--satip-playlist-url=https://cdn.hd-plus.de/api/channels/playlist.m3u"]];

    /* init our discoverer with the private library */
    _discoverer = [[VLCMediaDiscoverer alloc] initWithName:@"upnp" libraryInstance:_discoveryLibrary];
    /* enable debug logging here if desired */
    _discoverer.libraryInstance.debugLogging = NO;
    int i_ret = [_discoverer startDiscoverer];
    if (i_ret != 0) {
        if (self.delegate) {
            if ([self.delegate respondsToSelector:@selector(discoveryFailed)]) {
                [self.delegate discoveryFailed];
            }
        }
    }

    /* cache handle to server list and set ourselves as delegate so we get change events */
    _discoveredServerList = _discoverer.discoveredMedia;
    _discoveredServerList.delegate = self;
}

- (void)stopDiscovery
{
    /* stop discovery and dealloc everything but us */
    [_discoverer stopDiscoverer];
    _discoverer = nil;
    _discoveredServerList = nil;
    _discoveryLibrary = nil;
}

#pragma mark - media list delegation

- (void)mediaList:(VLCMediaList *)aMediaList mediaAdded:(VLCMedia *)media atIndex:(NSInteger)index
{
    /* notify delegate about a new server */
    if (self.delegate) {
        if ([self.delegate respondsToSelector:@selector(listOfServersWasUpdated)]) {
            [self.delegate listOfServersWasUpdated];
        }
    }
}

- (void)mediaList:(VLCMediaList *)aMediaList mediaRemovedAtIndex:(NSInteger)index
{
    /* notify delegate about a removed server */
    if (self.delegate) {
        if ([self.delegate respondsToSelector:@selector(listOfServersWasUpdated)]) {
            [self.delegate listOfServersWasUpdated];
        }
    }
}

#pragma mark - properties

- (VLCMedia *)serverAtIndex:(NSInteger)index
{
    return [_discoveredServerList mediaAtIndex:index];
}

- (NSInteger)numberOfServers
{
    return [_discoveredServerList count];
}

@end
