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

NSString *SESSelectedServerIndex = @"SESSelectedServerIndex";
NSString *SESCustomServers = @"SESCustomServers";
NSString *SESChannelListURLs = @"SESChannelListURLs";
NSString *SESChannelListURLNames = @"SESChannelListURLNames";
NSString *SESSelectedChannelListIndex = @"SESSelectedChannelListIndex";
NSString *SESLastChannelIndex = @"SESLastChannelIndex";

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

+ (void)initialize
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary *standardDefaults = @{ SESSelectedServerIndex : @(-1),
                                        SESCustomServers : @[],
                                        SESChannelListURLNames : @[@"Astra 19.2°E", @"Astra 28.2°E"],
                                        SESChannelListURLs : @[@"http://www.satip.info/Playlists/ASTRA_19_2E.m3u",
                                                               @"http://www.satip.info/Playlists/ASTRA_28_2E.m3u"],
                                        SESSelectedChannelListIndex : @(0),
                                        SESLastChannelIndex : @(0)};
    [defaults registerDefaults:standardDefaults];
}

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
        NSLog(@"%s", __PRETTY_FUNCTION__);
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        _customServers = [defaults arrayForKey:SESCustomServers];
        _playlistTitlesToChooseFrom = [defaults arrayForKey:SESChannelListURLNames];
        _playlistURLStringsToChooseFrom = [defaults arrayForKey:SESChannelListURLs];
        _selectedServerIndex = [defaults integerForKey:SESSelectedServerIndex];
        _selectedPlaylistIndex = [defaults integerForKey:SESSelectedChannelListIndex];
        _lastPlayedChannelIndex = [defaults integerForKey:SESLastChannelIndex];
    }
    return self;
}

- (void)startDiscovery
{
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

- (void)setCustomServers:(NSArray *)customServers
{
    _customServers = customServers;
    [[NSUserDefaults standardUserDefaults] setValue:customServers forKey:SESCustomServers];
}

- (void)setSelectedServerIndex:(NSInteger)selectedServerIndex
{
    _selectedServerIndex = selectedServerIndex;
    [[NSUserDefaults standardUserDefaults] setInteger:selectedServerIndex forKey:SESSelectedServerIndex];
}

- (void)setPlaylistTitlesToChooseFrom:(NSArray *)playlistTitlesToChooseFrom
{
    _playlistTitlesToChooseFrom = playlistTitlesToChooseFrom;
    [[NSUserDefaults standardUserDefaults] setObject:playlistTitlesToChooseFrom forKey:SESChannelListURLNames];
}

- (void)setPlaylistURLStringsToChooseFrom:(NSArray *)playlistURLStringsToChooseFrom
{
    _playlistURLStringsToChooseFrom = playlistURLStringsToChooseFrom;
    [[NSUserDefaults standardUserDefaults] setObject:playlistURLStringsToChooseFrom forKey:SESChannelListURLs];
}

- (void)setSelectedPlaylistIndex:(NSInteger)selectedPlaylistIndex
{
    _selectedPlaylistIndex = selectedPlaylistIndex;
    [[NSUserDefaults standardUserDefaults] setInteger:selectedPlaylistIndex forKey:SESSelectedChannelListIndex];
}

- (void)setLastPlayedChannelIndex:(NSInteger)lastPlayedChannelIndex
{
    _lastPlayedChannelIndex = lastPlayedChannelIndex;
    [[NSUserDefaults standardUserDefaults] setInteger:lastPlayedChannelIndex forKey:SESLastChannelIndex];
}

@end
