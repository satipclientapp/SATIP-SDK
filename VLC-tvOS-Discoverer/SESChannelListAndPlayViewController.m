//
//  SESChannelListAndPlayViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESChannelListAndPlayViewController.h"

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#define channelListReuseIdentifier @"channelListReuseIdentifier"

@interface SESChannelListAndPlayViewController () <UITableViewDataSource, UITableViewDelegate, VLCMediaPlayerDelegate>
{
    /* we have 1 player to download, process and report the playlist - it should be destroyed once this is done */
    VLCMediaPlayer *_parsePlayer;

    /* the list player we use for playback
     * see the API documentation for VLCMediaListPlayer, it is a more efficient way to handle playlists like the channel list
     * if you want to achieve fast and memory-efficient channel switches
     * for the API missing from VLCMediaPlayer, see that there is actually an instance exposed as a property with the full thing */
    VLCMediaListPlayer *_playbackPlayer;

    /* a cache of some view where we draw video in, so we can refer to it later */
    CGRect _initialVoutFrame;

    /* are we in our pseudo-fullscreen? */
    BOOL _fullscreen;
}
@end

@implementation SESChannelListAndPlayViewController

#pragma mark - setup

- (void)viewDidLoad {
    [super viewDidLoad];

    /* finish table view configuration */
    self.channelListTableView.dataSource = self;
    self.channelListTableView.delegate = self;

    /* cache the initial size of the view we draw video in for later use */
    _initialVoutFrame = self.videoOutputView.frame;

    // FIXME: add proper error handling if we don't have such an item (which should never happen)
    if (self.serverMediaItem) {
        /* setup our parse player, which we use to download the channel list and parse it */
        _parsePlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--play-and-stop"]];
        _parsePlayer.media = self.serverMediaItem;
        _parsePlayer.delegate = self;
        [_parsePlayer play];
    }
}

/* called when our channel list is ready
 * ideally filter the notification types and senders, so you can use this method
 * for both players and for more state management */
- (void)mediaPlayerStateChanged:(NSNotification *)aNotification
{
    [self.channelListTableView reloadData];
}

#pragma mark - visibility

- (void)viewWillAppear:(BOOL)animated
{
    [self.channelListTableView reloadData];
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    /* this is extremely important!
     * if the VC disappears, it will be released and therefore the player references
     * however, it is absolutely illegal to destroy the player before you stop
     * therefore, stop both here whatever happens */
    [_playbackPlayer stop];
    [_parsePlayer stop];
    [super viewWillDisappear:animated];
}

#pragma mark - table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (self.serverMediaItem) {
        return self.serverMediaItem.subitems.count;
    }

    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:channelListReuseIdentifier];

    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:channelListReuseIdentifier];
    }

    if (!self.serverMediaItem) {
        return cell;
    }

    VLCMediaList *subItems = self.serverMediaItem.subitems;
    NSInteger row = indexPath.row;

    if (subItems.count < row) {
        return cell;
    }

    VLCMedia *channelItem = [subItems mediaAtIndex:row];

    cell.textLabel.text = [channelItem metadataForKey:VLCMetaInformationTitle];

    return cell;
}

#pragma mark - table delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (!_playbackPlayer) {
        /* setup the playback list player if not already done */
        _playbackPlayer = [[VLCMediaListPlayer alloc] init];
        /* you can enable debug logging here ;) */
        _playbackPlayer.mediaPlayer.libraryInstance.debugLogging = NO;
        _playbackPlayer.mediaPlayer.drawable = self.videoOutputView;
        _playbackPlayer.mediaList = self.serverMediaItem.subitems;
    }

    /* and switch to the channel you want - this can be done repeatedly without destroying stuff over and over again */
    [_playbackPlayer playItemAtIndex:(int)indexPath.row];
}

#pragma mark - pseudo-fullscreen

- (IBAction)fullscreenAction:(id)sender
{
    /* clicker method for pseudo fullscreen */
    if (_fullscreen) {
        [self resizeVoutBackToSmall];
    } else {
        [self resizeVoutToFullscreen];
    }
    _fullscreen = !_fullscreen;
}

- (void)resizeVoutToFullscreen
{
    /* to demo the full video quality and as well as VLC's resizing capabilities during playback
     * this is a poor man's fullscreen
     * note that of course you can remove the view from the hierarchy and move it some place else like a new VC, etc. */
    [UIView animateWithDuration:1. animations:^{
        self.videoOutputView.frame = self.view.frame;
    }];
}

- (void)resizeVoutBackToSmall
{
    /* leave "fullscreen" by going back to the initial size */
    [UIView animateWithDuration:1. animations:^{
        self.videoOutputView.frame = _initialVoutFrame;
    }];
}

@end
