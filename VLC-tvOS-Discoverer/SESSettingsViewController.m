//
//  SESSettingsViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 12/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESSettingsViewController.h"
#import "SESTableViewCell.h"
#import "SESServerDiscoveryController.h"

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#import <AFNetworking/UIKit+AFNetworking.h>

NSString *SESServerListReUseIdentifier = @"SESServerListReUseIdentifier";
NSString *SESSatelliteListReUseIdentifier = @"SESSatelliteListReUseIdentifier";
NSString *SESChannelListReUseIdentifier = @"SESChannelListReUseIdentifier";

@interface SESSettingsViewController () <UITableViewDelegate, UITableViewDataSource, SESPlaybackControllerDelegate, VLCMediaPlayerDelegate, VLCMediaDelegate>
{
    /* we have 1 player to download, process and report the playlist - it should be destroyed once this is done */
    VLCMediaPlayer *_parsePlayer;
    VLCMedia *_playlistMediaItem;

    SESServerDiscoveryController *_discoveryController;
}
@end

@implementation SESSettingsViewController

- (NSString *)title
{
    return @"Settings";
}

#pragma mark - start and stop discovery depending on view visibility

- (void)viewDidLoad {
    [super viewDidLoad];

    /* init our discovery controller - note that it doesn't discover anything yet */
    _discoveryController = [SESServerDiscoveryController sharedDiscoveryController];
    _discoveryController.delegate = self;

#if TARGET_OS_TV
    self.serverListTableView.rowHeight = 100.;
    [self.serverListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:SESServerListReUseIdentifier];
    self.satelliteListTableView.rowHeight = 100.;
    [self.satelliteListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:SESSatelliteListReUseIdentifier];
    self.channelListTableView.rowHeight = 100.;
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:SESChannelListReUseIdentifier];
    self.titleLabel.textColor = [UIColor lightGrayColor];
    self.titleLabel.text = self.title;
#else
    self.serverListTableView.rowHeight = 68.;
    self.serverListTableView.tintColor = [UIColor clearColor];
    self.serverListTableView.separatorColor = [UIColor clearColor];
    [self.serverListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:SESServerListReUseIdentifier];
    self.satelliteListTableView.rowHeight = 68.;
    self.satelliteListTableView.tintColor = [UIColor clearColor];
    self.satelliteListTableView.separatorColor = [UIColor clearColor];
    [self.satelliteListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:SESSatelliteListReUseIdentifier];
    self.channelListTableView.rowHeight = 68.;
    self.channelListTableView.tintColor = [UIColor clearColor];
    self.channelListTableView.separatorColor = [UIColor clearColor];
    [self.channelListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:SESChannelListReUseIdentifier];
#endif

    self.serverListTableView.dataSource = self;
    self.serverListTableView.delegate = self;

    self.satelliteListTableView.dataSource = self;
    self.satelliteListTableView.delegate = self;

    self.channelListTableView.dataSource = self;
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.serverListTableView reloadData];

    [self.satelliteListTableView reloadData];

    [self parseCurrentChannelList];

    [super viewWillAppear:animated];

    [self.satelliteListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:_discoveryController.selectedPlaylistIndex inSection:0] animated:NO scrollPosition:UITableViewScrollPositionNone];
    [self.serverListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:_discoveryController.selectedServerIndex inSection:0] animated:NO scrollPosition:UITableViewScrollPositionNone];
}

- (void)viewWillDisappear:(BOOL)animated
{
    /* this is extremely important!
     * if the VC disappears, it will be released and therefore the player references
     * however, it is absolutely illegal to destroy the player before you stop
     * therefore, stop both here whatever happens */
    [_parsePlayer stop];
    _parsePlayer = nil;

    [super viewWillDisappear:animated];
}

- (void)parseCurrentChannelList
{
    if (_parsePlayer != nil) {
        [_parsePlayer stop];
        _parsePlayer = nil;
    }

    /* setup our parse player, which we use to download the channel list and parse it */
    NSURL *url = [NSURL URLWithString:_discoveryController.playlistURLStringsToChooseFrom[_discoveryController.selectedPlaylistIndex]];
    if (!url) {
        return;
    }
    _playlistMediaItem = [VLCMedia mediaWithURL:url];
    _parsePlayer = [[VLCMediaPlayer alloc] initWithOptions:@[@"--play-and-stop"]];
    _parsePlayer.media = _playlistMediaItem;
    _parsePlayer.delegate = self;
    /* you can enable debug logging here ;) */
    _parsePlayer.libraryInstance.debugLogging = NO;
    [_parsePlayer play];
}

- (IBAction)addServer:(id)sender
{
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Add server address"
                                                                             message:@"Enter the IP address or DNS name of an undiscovered SAT>IP server" preferredStyle:UIAlertControllerStyleAlert];

    __block UITextField *serverField = nil;
    [alertController addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        textField.placeholder = @"SAT>IP Server IP address or DNS name";
        serverField = textField;
    }];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Add Server"
                                                        style:UIAlertActionStyleDefault
                                                      handler:^(UIAlertAction * _Nonnull action) {
                                                          NSMutableArray *customServers = [_discoveryController.customServers mutableCopy];
                                                          [customServers addObject:serverField.text];
                                                          _discoveryController.customServers = [customServers copy];

                                                          [self.serverListTableView reloadData];
                                                      }]];

    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];
    
    [self presentViewController:alertController animated:YES completion:nil];
}

- (IBAction)addChannelList:(id)sender
{
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Add M3U Channel List"
                                                                             message:nil preferredStyle:UIAlertControllerStyleAlert];

    __block UITextField *urlField = nil;
    [alertController addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        textField.placeholder = @"M3U Channel List URL";
        urlField = textField;
    }];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Add Channel List URL"
                                                        style:UIAlertActionStyleDefault
                                                      handler:^(UIAlertAction * _Nonnull action) {
                                                          NSString *urlString = urlField.text;

                                                          NSMutableArray *mutArray = [_discoveryController.playlistURLStringsToChooseFrom mutableCopy];
                                                          [mutArray addObject:urlString];
                                                          _discoveryController.playlistURLStringsToChooseFrom = [mutArray copy];

                                                          mutArray = [_discoveryController.playlistTitlesToChooseFrom mutableCopy];
                                                          [mutArray addObject:[urlString lastPathComponent]];
                                                          _discoveryController.playlistTitlesToChooseFrom = [mutArray copy];

                                                          [self.satelliteListTableView reloadData];
                                                      }]];

    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];

    [self presentViewController:alertController animated:YES completion:nil];
}

#pragma mark - table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (tableView == self.satelliteListTableView) {
        return  _discoveryController.playlistTitlesToChooseFrom.count;
    } else if (tableView == self.serverListTableView) {
        return _discoveryController.numberOfServers + _discoveryController.customServers.count;
    } else {
        if (_playlistMediaItem) {
            return _playlistMediaItem.subitems.count;
        }
    }

    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    SESTableViewCell *cell;
    NSInteger index = indexPath.row;

    if (tableView == self.serverListTableView) {
        cell = [self.serverListTableView dequeueReusableCellWithIdentifier:SESServerListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [SESTableViewCell new];
        }

        NSInteger serverCount = _discoveryController.numberOfServers;
        if (index < serverCount) {
            VLCMedia *media = [_discoveryController serverAtIndex:index];
            if (!media) {
                cell.channelNameLabel.text = @"bad server";
                return cell;
            }

            cell.channelNameLabel.text = [media metadataForKey:VLCMetaInformationTitle];
        } else {
            cell.channelNameLabel.text = _discoveryController.customServers[index - serverCount];
        }
        cell.channelIconImageView.image = [UIImage imageNamed:@"logo"];

        return cell;
    } else if (tableView == self.satelliteListTableView) {
        SESTableViewCell *cell = [self.satelliteListTableView dequeueReusableCellWithIdentifier:SESSatelliteListReUseIdentifier];
        if (!cell) {
            cell = [SESTableViewCell new];
        }
        cell.channelNameLabel.text = _discoveryController.playlistTitlesToChooseFrom[indexPath.row];
        cell.channelIconImageView.image = [UIImage imageNamed:@"logo"];
        return cell;
    } else {
        cell = [self.channelListTableView dequeueReusableCellWithIdentifier:SESChannelListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [SESTableViewCell new];
        }

        cell.channelIconImageView.image = nil;

        if (!_playlistMediaItem) {
            return cell;
        }

        VLCMediaList *subItems = _playlistMediaItem.subitems;
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

    return cell;
}

#pragma mark - table view delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (tableView == self.satelliteListTableView) {
        _discoveryController.selectedPlaylistIndex = indexPath.row;
        [self parseCurrentChannelList];
    } else {
        _discoveryController.selectedServerIndex = indexPath.row;
    }
}

#pragma mark - discovery controller delegate

- (void)listOfServersWasUpdated
{
    [self.serverListTableView reloadData];
}

- (void)discoveryFailed
{
    NSLog(@"Discovery of SAT>IP devices failed");
}

#pragma mark - VLC media delegation

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


@end
