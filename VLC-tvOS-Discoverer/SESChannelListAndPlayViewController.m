//
//  SESChannelListAndPlayViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESChannelListAndPlayViewController.h"
#import "SESTableViewCell.h"
#import "SESServerDiscoveryController.h"
#import "UIColor+SES.h"
#import "SESSettingsViewController.h"

#import <AFNetworking/UIKit+AFNetworking.h>

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#define channelListReuseIdentifier @"channelListReuseIdentifier"

@interface SESChannelListAndPlayViewController () <UITableViewDataSource, UITableViewDelegate, VLCMediaPlayerDelegate, VLCMediaDelegate, UIGestureRecognizerDelegate, SESPlaybackControllerDelegate>
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

    NSSet<UIGestureRecognizer *> *_simultaneousGestureRecognizers;

    SESServerDiscoveryController *_discoveryController;

    BOOL _automaticallyStarted;
    BOOL _appBackgrounded;

    BOOL _swipeChannelIndexDelay;
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

    _discoveryController = [SESServerDiscoveryController sharedDiscoveryController];
    _discoveryController.delegate = self;

    /* finish table view configuration */
    self.channelListTableView.dataSource = self;
    self.channelListTableView.delegate = self;
    self.channelListTableView.backgroundColor = [UIColor clearColor];

    UISwipeGestureRecognizer *leftSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(swipeLeftAction)];
    leftSwipeRecognizer.direction = UISwipeGestureRecognizerDirectionLeft;

    UISwipeGestureRecognizer *rightSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(swipeRightAction)];
    rightSwipeRecognizer.direction = UISwipeGestureRecognizerDirectionRight;

#if TARGET_OS_TV
    self.channelListTableView.rowHeight = 100.;
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:channelListReuseIdentifier];
    UIImage *whiteButtonBackgroundImage = [UIColor sesImageWithColor:[UIColor whiteColor]];
    [self.fullscreenButton setBackgroundImage:whiteButtonBackgroundImage forState:UIControlStateNormal];
    [self.fullscreenButton setBackgroundImage:whiteButtonBackgroundImage forState:UIControlStateFocused];

    [self.view addGestureRecognizer:leftSwipeRecognizer];
    [self.view addGestureRecognizer:rightSwipeRecognizer];

    leftSwipeRecognizer.delegate = self;
    rightSwipeRecognizer.delegate = self;

    NSMutableSet<UIGestureRecognizer *> *simultaneousGestureRecognizers = [NSMutableSet set];
    [simultaneousGestureRecognizers addObject:leftSwipeRecognizer];
    [simultaneousGestureRecognizers addObject:rightSwipeRecognizer];
    _simultaneousGestureRecognizers = simultaneousGestureRecognizers;
#else
    self.channelListTableView.rowHeight = 68.;
    self.channelListTableView.tintColor = [UIColor clearColor];
    self.channelListTableView.separatorColor = [UIColor clearColor];
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:channelListReuseIdentifier];

    UITapGestureRecognizer *tapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(fullscreenAction:)];
    [self.videoOutputView addGestureRecognizer:tapRecognizer];

    [self.videoOutputView addGestureRecognizer:leftSwipeRecognizer];
    [self.videoOutputView addGestureRecognizer:rightSwipeRecognizer];
#endif

    /* cache the initial size of the view we draw video in for later use */
    _initialVoutContraints = self.videoOutputView.constraints;

    NSDictionary *dict = NSDictionaryOfVariableBindings(_videoOutputView);
    _horizontalFullscreenVoutContraints = [NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_videoOutputView]|" options:0 metrics:0 views:dict];
    _verticalFullscreenVoutContraints = [NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_videoOutputView]|" options:0 metrics:0 views:dict];

    NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
    [notificationCenter addObserver:self
                           selector:@selector(appWillGoToBackground)
                               name:UIApplicationWillResignActiveNotification
                             object:nil];
    [notificationCenter addObserver:self
                           selector:@selector(appWillGoToForeground)
                               name:UIApplicationWillEnterForegroundNotification
                             object:nil];
    [notificationCenter addObserver:self
                           selector:@selector(resetPlaybackEngine)
                               name:SESSettingsMajorPlaybackConfigurationChange
                             object:nil];
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];

    /* this is extremely important!
     * if the VC is released and therefore the player references,
     * it is absolutely illegal to destroy the player before you stop
     * therefore, stop both here whatever happens */

    if (_playbackPlayer) {
        [_playbackPlayer stop];
        _playbackPlayer = nil;
    }

    if (_parsePlayer) {
        [_parsePlayer stop];
        _parsePlayer = nil;
    }
}

- (void)appWillGoToBackground
{
    if (_playbackPlayer) {
        _appBackgrounded = YES;
        [_playbackPlayer.mediaPlayer stop];
    }
}

- (void)appWillGoToForeground
{
    if (!_appBackgrounded) {
        return;
    }

    _appBackgrounded = NO;
    [self automaticPlaybackStart];
}

- (void)resetPlaybackEngine
{
    _discoveryController.lastPlayedChannelIndex = 0;

    if (_playbackPlayer) {
        [_playbackPlayer stop];
        _playbackPlayer = nil;
    }

    if (_parsePlayer) {
        [_parsePlayer stop];
        _parsePlayer = nil;
    }

    _serverMediaItem = nil;

    _appBackgrounded = NO;

    _automaticallyStarted = NO;
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

    if (!_automaticallyStarted) {
        [self performSelector:@selector(automaticPlaybackStart) withObject:nil afterDelay:2.];
    }
}

#pragma mark - visibility

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];

    if (_appBackgrounded) {
        [self appWillGoToForeground];
        return;
    }

    [self startPlayback];
}

- (void)startPlayback
{
    NSInteger index = _discoveryController.selectedServerIndex;

    if (index == -1) {
        NSLog(@"%s: No server selected", __PRETTY_FUNCTION__);
        return;
    }

    NSInteger serverCount = _discoveryController.numberOfServers;
    if (serverCount == 0 && _discoveryController.customServers.count == 0) {
        NSLog(@"%s: No server found", __PRETTY_FUNCTION__);
        return;
    }

    VLCMedia *serverItem;
    NSString *playlistURLString = _discoveryController.playlistURLStringsToChooseFrom[_discoveryController.selectedPlaylistIndex];
    NSString *ipString;

    if (index < serverCount) {
        serverItem = [_discoveryController serverAtIndex:index];
    } else {
        NSInteger customerServerIndex = index - serverCount;
        if (customerServerIndex < _discoveryController.customServers.count) {
            ipString = _discoveryController.customServers[index - serverCount];
        }

        if (ipString == nil) {
            NSLog(@"%s: Server parsing failure", __PRETTY_FUNCTION__);
            return;
        }

        NSString *mrl = playlistURLString;

        serverItem = [VLCMedia mediaWithURL:[NSURL URLWithString:mrl]];
        [serverItem addOptions:@{ @"satip-host" : ipString}];
    }

    _serverMediaItem = serverItem;

    // FIXME: add proper error handling if we don't have such an item (which should never happen)
    if (self.serverMediaItem) {
        self.serverMediaItem.delegate = self;

        if (_parsePlayer) {
            [_parsePlayer stop];
            _parsePlayer = nil;
        }

        /* setup our parse player, which we use to download the channel list and parse it */
        _parsePlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--play-and-stop"]];
        _parsePlayer.media = self.serverMediaItem;
        _parsePlayer.delegate = self;
        /* you can enable debug logging here ;) */
        // _parsePlayer.libraryInstance.debugLogging = YES;
        [_parsePlayer play];
    }

    [self.channelListTableView reloadData];

    if (_playbackPlayer) {
        [_playbackPlayer stop];
        _playbackPlayer = nil;
    }

    /* setup the playback list player if not already done */
#if TARGET_OS_TV
    _playbackPlayer = [[VLCMediaListPlayer alloc] init];
#else
    _playbackPlayer = [[VLCMediaListPlayer alloc] initWithOptions:@[@"videotoolbox-zero-copy"]];
#endif
    /* you can enable debug logging here ;) */
//    _playbackPlayer.mediaPlayer.libraryInstance.debugLogging = YES;
    _playbackPlayer.mediaPlayer.drawable = self.videoOutputView;
    _playbackPlayer.mediaList = self.serverMediaItem.subitems;
}

- (void)automaticPlaybackStart
{
    /* automatic playback start */
    NSInteger channelIndex = _discoveryController.lastPlayedChannelIndex;
    [self.channelListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:channelIndex inSection:0] animated:YES scrollPosition:UITableViewScrollPositionNone];
    [_playbackPlayer playItemAtNumber:@(channelIndex)];
    _automaticallyStarted = YES;
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    [self appWillGoToBackground];
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
    cell.channelIconImageView.image = nil;

    if (!self.serverMediaItem) {
        return cell;
    }
    
    VLCMediaList *subItems = self.serverMediaItem.subitems;
    NSInteger row = indexPath.row;
    
    if (subItems.count < row) {
        return cell;
    }
    
    VLCMedia *channelItem = [subItems mediaAtIndex:row];
    NSString *channelTitle;
    if (channelItem.parsedStatus != VLCMediaParsedStatusFailed || channelItem.parsedStatus == VLCMediaParsedStatusInit) {
        channelTitle = [channelItem metadataForKey:VLCMetaInformationTitle];
    } else {
        channelItem.delegate = self;
        [channelItem parseWithOptions:VLCMediaParseNetwork | VLCMediaParseLocal];
    }
    cell.channelNameLabel.text = channelTitle;

    NSRange dotRange = [channelTitle rangeOfString:@". "];
    channelTitle = [channelTitle substringFromIndex: (dotRange.location+dotRange.length)];
    channelTitle = [channelTitle stringByReplacingOccurrencesOfString:@" " withString:@"-"];
    channelTitle = [channelTitle stringByReplacingOccurrencesOfString:@"." withString:@"-"];
    channelTitle = [channelTitle lowercaseString];
    NSString *logourl = [NSString stringWithFormat:@"http://www.satip.info/Playlists/Channellogos/%@.png", channelTitle];
    [cell.channelIconImageView setImageWithURL:[NSURL URLWithString:logourl]];

    return cell;
}

#pragma mark - table delegation

#if TARGET_OS_TV
- (NSIndexPath *)indexPathForPreferredFocusedViewInTableView:(UITableView *)tableView
{
    NSInteger lastIndex = [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex;
    return [NSIndexPath indexPathForRow:lastIndex inSection:0];
}
#endif

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSInteger row = indexPath.row;

    /* reject channel switch to the same channel when we are still buffering */
    if (row == [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex && _playbackPlayer.mediaPlayer.state == VLCMediaPlayerStateBuffering) {
        return;
    }

    /* and switch to the channel you want - this can be done repeatedly without destroying stuff over and over again */
    [_playbackPlayer playItemAtNumber:@(row)];
    [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex = row;
}

- (void)swipeToIndex:(NSInteger)index
{
    [self.channelListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:index inSection:0] animated:NO scrollPosition:UITableViewScrollPositionMiddle];
    [_playbackPlayer playItemAtNumber:@(index)];
    [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex = index;
}

- (void)swipeRightAction
{
#if TARGET_OS_TV
    if (!_fullscreen) {
        return;
    }
#endif

    NSInteger index = self.channelListTableView.indexPathForSelectedRow.row;
    if (index == 0)
        return;

    index = index - 1;

    if (_swipeChannelIndexDelay) {
        return;
    }

    _swipeChannelIndexDelay = YES;

    double amountOfSeconds = 0.3;
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(amountOfSeconds * NSEC_PER_SEC));
    dispatch_after(delayTime, dispatch_get_main_queue(), ^{
        _swipeChannelIndexDelay = NO;
        [self swipeToIndex:index];
    });
}

- (void)swipeLeftAction
{
#if TARGET_OS_TV
    if (!_fullscreen) {
        return;
    }
#endif

    NSInteger index = self.channelListTableView.indexPathForSelectedRow.row;
    if (index == (self.serverMediaItem.subitems.count - 1))
        return;

    index = index + 1;

    if (_swipeChannelIndexDelay) {
        return;
    }

    _swipeChannelIndexDelay = YES;

    double amountOfSeconds = 0.3;
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(amountOfSeconds * NSEC_PER_SEC));
    dispatch_after(delayTime, dispatch_get_main_queue(), ^{
        _swipeChannelIndexDelay = NO;
        [self swipeToIndex:index];
    });
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

- (IBAction)dismissScreen:(id)sender
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - gesture recognizer delegate

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer
{
    return YES;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer
{
    return [_simultaneousGestureRecognizers containsObject:gestureRecognizer];
}

#pragma mark - SESPlaybackControllerDelegate
- (void)listOfServersWasUpdated
{
    if (!_playbackPlayer) {
        [self startPlayback];
    }
}

- (void)discoveryFailed
{
    NSLog(@"Server discovery failed");
}

@end
