//
//  SESServerSelectViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 17/03/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESServerSelectViewController.h"
#import "SESServerDiscoveryController.h"
#import "SESChannelListAndPlayViewController.h"

/* include the correct VLCKit fork per platform */
#if TARGET_OS_TV
#import <DynamicTVVLCKit/DynamicTVVLCKit.h>
#else
#import <DynamicMobileVLCKit/DynamicMobileVLCKit.h>
#endif

#define reuseableIdentifierForServer @"reuseableIdentifierForServer"

@interface SESServerSelectViewController () <UITableViewDataSource, UITableViewDelegate, SESPlaybackControllerDelegate>
{
    /* our discovery controller */
    SESServerDiscoveryController *_discoveryController;
}
@end

@implementation SESServerSelectViewController

#pragma mark - setup

- (void)viewDidLoad {
    [super viewDidLoad];

    /* init our discovery controller - note that it doesn't discover anything yet */
    _discoveryController = [[SESServerDiscoveryController alloc] init];
    _discoveryController.delegate = self;

    /* finish table view setup */
    self.serverTableView.dataSource = self;
    self.serverTableView.delegate = self;

    [self.serverTableView reloadData];
}

#pragma mark - start and stop discovery depending on view visibility

- (void)viewWillAppear:(BOOL)animated
{
    /* do the expensive call of starting the discovery - should be done once only */
    [_discoveryController startDiscovery];
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    /* stop discovery as soon as we don't need it anymore */
    [_discoveryController stopDiscovery];
    [super viewWillDisappear:animated];
}

#pragma mark - table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _discoveryController.numberOfServers;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:reuseableIdentifierForServer];
    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseableIdentifierForServer];
    }

    VLCMedia *media = [_discoveryController serverAtIndex:indexPath.row];
    if (!media) {
        cell.textLabel.text = @"bad server";
        return cell;
    }

    cell.textLabel.text = [media metadataForKey:VLCMetaInformationTitle];

    return cell;
}

#pragma mark - table view delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    /* FIXME: evil hack here, violation of MVC */
    UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
    // FIXME: this is an extremely bad way to differenciate between SAT>IP hosts and regular UPnP boxes - proper fix requires VLCKit amendment, which will come
    if ([cell.textLabel.text rangeOfString:@"Quincy"].location != NSNotFound) {
        SESChannelListAndPlayViewController *playVC;
        /* load the correct UI depending on the platform */
#if TARGET_OS_TV
        playVC = [[SESChannelListAndPlayViewController alloc] initWithNibName:nil bundle:nil];
#else
        playVC = [[SESChannelListAndPlayViewController alloc] initWithNibName:@"SESChannelListAndPlayViewController-iPad" bundle:nil];
#endif
        /* forward our server media item to the playback view controller
         * after this point, the discovery controller can be safely destroyed
         */
        playVC.serverMediaItem = [_discoveryController serverAtIndex:indexPath.row];
        [self presentViewController:playVC animated:YES completion:nil];
    } else
        NSLog(@"Invalid server");
}

#pragma mark - discovery controller delegate

- (void)listOfServersWasUpdated
{
    [self.serverTableView reloadData];
}

@end
