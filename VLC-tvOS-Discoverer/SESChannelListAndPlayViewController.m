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

#import <AFNetworking/UIKit+AFNetworking.h>

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#define channelListReuseIdentifier @"channelListReuseIdentifier"

@interface SESChannelListAndPlayViewController () <UITableViewDataSource, UITableViewDelegate, VLCMediaPlayerDelegate, VLCMediaDelegate, UIGestureRecognizerDelegate>
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
}

@end

@implementation SESChannelListAndPlayViewController

#pragma mark - setup

- (NSString *)title
{
    return @"Playback";
}

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

    UISwipeGestureRecognizer *leftSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(previousChannel)];
    leftSwipeRecognizer.direction = UISwipeGestureRecognizerDirectionLeft;

    UISwipeGestureRecognizer *rightSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(nextChannel)];
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

    [_playbackPlayer stop];
    _playbackPlayer = nil;

    /* setup the playback list player if not already done */
    _playbackPlayer = [[VLCMediaListPlayer alloc] init];
    /* you can enable debug logging here ;) */
    _playbackPlayer.mediaPlayer.libraryInstance.debugLogging = NO;
    _playbackPlayer.mediaPlayer.drawable = self.videoOutputView;
    _playbackPlayer.mediaList = self.serverMediaItem.subitems;

    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    /* automatic playback start */
    NSInteger index = [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex;
    [self.channelListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:index inSection:0] animated:animated scrollPosition:UITableViewScrollPositionNone];
    [_playbackPlayer playItemAtIndex:(int)index];
    [super viewDidAppear:animated];
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
    NSString *logourl = [NSString stringWithFormat:@"http://www.satip.info/sites/satip/files/files/Playlists/Channellogos/%@.png", channelTitle];
    [cell.channelIconImageView setImageWithURL:[NSURL URLWithString:logourl]];

    return cell;
}

#pragma mark - table delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSInteger row = indexPath.row;
    /* and switch to the channel you want - this can be done repeatedly without destroying stuff over and over again */
    [_playbackPlayer playItemAtIndex:(int)row];
    [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex = row;
}

- (void)previousChannel
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
    [self.channelListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:index inSection:0] animated:NO scrollPosition:UITableViewScrollPositionMiddle];
    [_playbackPlayer playItemAtIndex:(int)index];
    [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex = index;
}

- (void)nextChannel
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
    [self.channelListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:index inSection:0] animated:NO scrollPosition:UITableViewScrollPositionMiddle];
    [_playbackPlayer playItemAtIndex:(int)index];
    [SESServerDiscoveryController sharedDiscoveryController].lastPlayedChannelIndex = index;
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

@end
