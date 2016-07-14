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
#import "UIColor+SES.h"

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
    _discoveryController = [SESServerDiscoveryController sharedDiscoveryController];
    _discoveryController.delegate = self;

    /* finish table view setup */
    self.serverTableView.dataSource = self;
    self.serverTableView.delegate = self;

#if TARGET_OS_TV
    self.serverTableView.rowHeight = 100.;
#else
    self.serverTableView.rowHeight = 68.;
    self.serverTableView.separatorColor = [UIColor clearColor];
#endif

    self.viewControllerTitleLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:38.];

    CAGradientLayer *gradient = [CAGradientLayer layer];
    gradient.frame = self.view.bounds;
    gradient.colors = [NSArray arrayWithObjects:(id)[[UIColor sesPureWhite]CGColor], (id)[[UIColor sesPearlColor]CGColor], nil];
    [gradient setStartPoint:CGPointMake(1, 1)];
    [gradient setEndPoint:CGPointMake(0, 0)];
    [self.view.layer insertSublayer:gradient atIndex:0];
}

- (NSString *)title
{
    return @"Server Selection";
}

#pragma mark - start and stop discovery depending on view visibility

- (void)viewWillAppear:(BOOL)animated
{
    [self.serverTableView reloadData];

    [super viewWillAppear:animated];

    NSInteger selectedServer = _discoveryController.selectedServerIndex;

    [self.serverTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:selectedServer inSection:0] animated:NO scrollPosition:UITableViewScrollPositionNone];
}

#pragma mark - table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _discoveryController.numberOfServers + _discoveryController.customServers.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:reuseableIdentifierForServer];
    NSInteger index = indexPath.row;

    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseableIdentifierForServer];
        cell.backgroundColor = [UIColor sesPureWhite];
#if TARGET_OS_TV
        cell.textLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:29.];
#else
        cell.textLabel.font = [UIFont fontWithName:@"HelveticaNeue-Light" size:21.];
#endif
    }

    NSInteger serverCount = _discoveryController.numberOfServers;
    if (index < serverCount) {
        VLCMedia *media = [_discoveryController serverAtIndex:index];
        if (!media) {
            cell.textLabel.text = @"bad server";
            return cell;
        }

        cell.textLabel.text = [media metadataForKey:VLCMetaInformationTitle];
    } else {
        cell.textLabel.text = _discoveryController.customServers[index - serverCount];
    }

    return cell;
}

#pragma mark - table view delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
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

    NSInteger index = indexPath.row;
    VLCMedia *serverItem;
    NSString *playlistURLString = _discoveryController.playlistURLStringsToChooseFrom[_discoveryController.selectedPlaylistIndex];
    NSString *ipString;

    NSInteger serverCount = _discoveryController.numberOfServers;
    if (index < serverCount) {
        NSURLComponents *components = [NSURLComponents componentsWithURL:[_discoveryController serverAtIndex:_discoveryController.selectedServerIndex].url resolvingAgainstBaseURL:NO];
        ipString = [components.queryItems.firstObject value];
    } else {
        ipString = _discoveryController.customServers[index - serverCount];
    }

    NSString *mrl = [playlistURLString stringByReplacingOccurrencesOfString:@"http://" withString:@"http/lua://"];
    mrl = [mrl stringByReplacingOccurrencesOfString:@"https://" withString:@"https/lua://"];
    mrl = [mrl stringByAppendingFormat:@"?device=%@", ipString];

    serverItem = [VLCMedia mediaWithURL:[NSURL URLWithString:mrl]];

    playVC.serverMediaItem = serverItem;

    [self presentViewController:playVC animated:YES completion:nil];
}

#pragma mark - discovery controller delegate

- (void)listOfServersWasUpdated
{
    [self.serverTableView reloadData];
}

- (void)discoveryFailed
{
    NSLog(@"Discovery of SAT>IP devices failed");
}

@end
