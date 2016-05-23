//
//  SESChannelListAndPlayViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESChannelListAndPlayViewController.h"
#import "SESTableViewCell.h"
#import "SESColors.h"
#import "SESFocusView.h"

#import <AFNetworking/UIKit+AFNetworking.h>

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#define channelListReuseIdentifier @"channelListReuseIdentifier"

@interface SESChannelListAndPlayViewController () <UITableViewDataSource, UITableViewDelegate, VLCMediaPlayerDelegate, VLCMediaDelegate>
{
    /* we have 1 player to download, process and report the playlist - it should be destroyed once this is done */
    VLCMediaPlayer *_parsePlayer;
    
    /* the list player we use for playback
     * see the API documentation for VLCMediaListPlayer, it is a more efficient way to handle playlists like the channel list
     * if you want to achieve fast and memory-efficient channel switches
     * for the API missing from VLCMediaPlayer, see that there is actually an instance exposed as a property with the full thing */
    VLCMediaListPlayer *_playbackPlayer;
    
    /* a cache of some view where we draw video in, so we can refer to it later */
    NSArray *_initialVoutContraints;
    NSArray *_horizontalFullscreenVoutContraints;
    NSArray *_verticalFullscreenVoutContraints;


    /* are we in our pseudo-fullscreen? */
    BOOL _fullscreen;

#if TARGET_OS_TV
    SESFocusView *_focusView;
#endif
}

@end

@implementation SESChannelListAndPlayViewController

#pragma mark - setup

- (UIView *)preferredFocusedView
{
    return self.fullscreenButton;
}

- (void)viewDidLoad {
    [super viewDidLoad];

    /* finish table view configuration */
    self.channelListTableView.dataSource = self;
    self.channelListTableView.delegate = self;
    self.channelListTableView.backgroundColor = [UIColor clearColor];

#if TARGET_OS_TV
    self.channelListTableView.rowHeight = 100.;
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:channelListReuseIdentifier];

    CGRect tableViewFrame = self.channelListTableView.frame;
    _focusView = [[SESFocusView alloc] initWithFrame:CGRectMake(tableViewFrame.origin.x + tableViewFrame.size.width, tableViewFrame.origin.y, 300., tableViewFrame.size.height)];
    _focusView.backgroundColor = [UIColor clearColor];

    [self.view addSubview:_focusView];
#else
    self.channelListTableView.rowHeight = 68.;
    self.channelListTableView.tintColor = [UIColor clearColor];
    self.channelListTableView.separatorColor = [UIColor clearColor];
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:channelListReuseIdentifier];
#endif
    
    /* cache the initial size of the view we draw video in for later use */
    _initialVoutContraints = self.videoOutputView.constraints;

    NSDictionary *dict = NSDictionaryOfVariableBindings(_videoOutputView);
    _horizontalFullscreenVoutContraints = [NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_videoOutputView]|" options:0 metrics:0 views:dict];
    _verticalFullscreenVoutContraints = [NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_videoOutputView]|" options:0 metrics:0 views:dict];

    // FIXME: add proper error handling if we don't have such an item (which should never happen)
    if (self.serverMediaItem) {
        self.serverMediaItem.delegate = self;

        /* setup our parse player, which we use to download the channel list and parse it */
        _parsePlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--play-and-stop"]];
        _parsePlayer.media = self.serverMediaItem;
        _parsePlayer.delegate = self;
        /* you can enable debug logging here ;) */
        _parsePlayer.libraryInstance.debugLogging = NO;
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

- (void)mediaDidFinishParsing:(VLCMedia *)aMedia
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
    SESTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:channelListReuseIdentifier];

    if (!cell) {
        cell = [SESTableViewCell new];
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
    NSString *str;
    if (channelItem.isParsed) {
        str = [channelItem metadataForKey:VLCMetaInformationTitle];
    } else {
        channelItem.delegate = self;
        [channelItem parseWithOptions:VLCMediaParseNetwork | VLCMediaParseLocal | VLCMediaFetchNetwork];
    }
    NSArray<NSString *> *splitName = [str componentsSeparatedByString:@";"];
    if (splitName.count > 1)
    {
        NSString *address = [splitName[1] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
        NSURL *URL = [NSURL URLWithString:address relativeToURL:[NSURL URLWithString:@"https://cdn.hd-plus.de"]];
    
        cell.channelNameLabel.text = splitName[0];
        [cell.channelIconImageView setImageWithURL:URL];
    } else {
        cell.channelNameLabel.text = str;
    }

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
    [self.fullscreenButton setSelected:![self.fullscreenButton isSelected]];

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
    [NSLayoutConstraint deactivateConstraints:_initialVoutContraints];
    [NSLayoutConstraint activateConstraints:_horizontalFullscreenVoutContraints];
    [NSLayoutConstraint activateConstraints:_verticalFullscreenVoutContraints];
    [UIView animateWithDuration:1. animations:^{
        [self.view layoutIfNeeded];
        [self.view bringSubviewToFront:self.videoOutputView];
        [self.view bringSubviewToFront:self.fullscreenButton];
    }];
}

- (void)resizeVoutBackToSmall
{
    /* leave "fullscreen" by going back to the initial size */
    [NSLayoutConstraint deactivateConstraints:_horizontalFullscreenVoutContraints];
    [NSLayoutConstraint deactivateConstraints:_verticalFullscreenVoutContraints];
    [NSLayoutConstraint activateConstraints:_initialVoutContraints];
    [UIView animateWithDuration:1. animations:^{
        [self.view layoutIfNeeded];
    }];
}
@end
