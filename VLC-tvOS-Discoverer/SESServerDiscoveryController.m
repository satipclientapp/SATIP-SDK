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

    NSMutableArray *_filteredServerList;

    BOOL _discoveryFailed;
}
@end

@implementation SESServerDiscoveryController

+ (instancetype)sharedDiscoveryController
{
    static SESServerDiscoveryController *sharedInstance = nil;
    static dispatch_once_t pred;

    dispatch_once(&pred, ^{
        sharedInstance = [[SESServerDiscoveryController alloc] init];
    });

    return sharedInstance;
}

#pragma mark - setup and destruction

- (instancetype)init
{
    self = [super init];
    if (self) {
        _playlistTitlesToChooseFrom = @[@"Astra 19°2E", @"Astra 28°2E", @"Astra 23°5E"];
        _playlistURLStringsToChooseFrom = @[@"http://www.satip.info/Playlists/ASTRA_19_2E.m3u",
                                            @"http://www.satip.info/Playlists/ASTRA_28_2E.m3u",
                                            @"http://www.satip.info/Playlists/ASTRA_23_5E.m3u"];
        _customServers = @[];
    }
    return self;
}

- (void)startDiscovery
{
    NSLog(@"%s", __PRETTY_FUNCTION__);
    _filteredServerList = [NSMutableArray array];

    _discoveryLibrary = [VLCLibrary sharedLibrary];

    /* init our discoverer with the private library */
    _discoverer = [[VLCMediaDiscoverer alloc] initWithName:@"upnp" libraryInstance:_discoveryLibrary];
    /* enable debug logging here if desired */
    _discoverer.libraryInstance.debugLogging = NO;
    _discoveryFailed = NO;
    int i_ret = [_discoverer startDiscoverer];
    if (i_ret != 0) {
        _discoveryFailed = YES;
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
    NSLog(@"%s", __PRETTY_FUNCTION__);
    /* stop discovery and dealloc everything but us */
    if (!_discoveryFailed) {
        [_discoverer stopDiscoverer];
    }
    _discoverer = nil;
    _discoveredServerList = nil;
    _discoveryLibrary = nil;
}

#pragma mark - media list delegation

- (void)recreateFilteredList
{
    [_filteredServerList removeAllObjects];

    NSUInteger count = [_discoveredServerList count];
    for (NSUInteger x = 0; x < count; x++) {
        VLCMedia *media = [_discoveredServerList mediaAtIndex:x];
        if ([[media metadataForKey:VLCMetaInformationSetting] isEqualToString:@"urn:ses-com:device:SatIPServer:1"]) {
            NSLog(@"media url: %@", media.url);
            [_filteredServerList addObject:media];
        }
    }

    if (self.delegate) {
        if ([self.delegate respondsToSelector:@selector(listOfServersWasUpdated)]) {
            [self.delegate listOfServersWasUpdated];
        }
    }
}

- (void)mediaList:(VLCMediaList *)aMediaList mediaAdded:(VLCMedia *)media atIndex:(NSInteger)index
{
    /* notify delegate about a new server */
    [self recreateFilteredList];
}

- (void)mediaList:(VLCMediaList *)aMediaList mediaRemovedAtIndex:(NSInteger)index
{
    /* notify delegate about a removed server */
    [self recreateFilteredList];
}

#pragma mark - properties

- (VLCMedia *)serverAtIndex:(NSInteger)index
{
    return _filteredServerList[index];
}

- (NSInteger)numberOfServers
{
    return _filteredServerList.count;
}

@end
