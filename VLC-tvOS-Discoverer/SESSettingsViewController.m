//
//  SESSettingsViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 12/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESSettingsViewController.h"
#import "SESTableViewCell.h"

NSString *SESServerListReUseIdentifier = @"SESServerListReUseIdentifier";
NSString *SESSatelliteListReUseIdentifier = @"SESSatelliteListReUseIdentifier";
NSString *SESChannelListReUseIdentifier = @"SESChannelListReUseIdentifier";

@interface SESSettingsViewController () <UITableViewDelegate, UITableViewDataSource>
{
    NSArray *_playlistTitlesToChooseFromArray;
    NSArray *_playlistURLStringsToChooseFromArray;
}
@end

@implementation SESSettingsViewController

- (NSString *)title
{
    return @"Settings";
}

- (void)viewDidLoad {
    [super viewDidLoad];

#if TARGET_OS_TV
    self.satelliteListTableView.rowHeight = 100.;
    [self.satelliteListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell" bundle:nil] forCellReuseIdentifier:SESSatelliteListReUseIdentifier];
#else
    self.satelliteListTableView.rowHeight = 68.;
    self.satelliteListTableView.tintColor = [UIColor clearColor];
    self.satelliteListTableView.separatorColor = [UIColor clearColor];
    [self.satelliteListTableView registerNib:[UINib nibWithNibName:@"SESTableViewCell-ipad" bundle:nil] forCellReuseIdentifier:SESSatelliteListReUseIdentifier];
#endif

    self.serverListTableView.dataSource = self;
    self.serverListTableView.delegate = self;
    [self.serverListTableView registerClass:[UITableViewCell class] forCellReuseIdentifier:SESServerListReUseIdentifier];

    self.satelliteListTableView.dataSource = self;
    self.satelliteListTableView.delegate = self;

    self.channelListTableView.dataSource = self;
    [self.channelListTableView registerClass:[UITableViewCell class] forCellReuseIdentifier:SESChannelListReUseIdentifier];

    _playlistTitlesToChooseFromArray = @[@"Astra 19°2E", @"Astra 28°2E", @"Astra 23°5E"];
    _playlistURLStringsToChooseFromArray = @[@"http://www.satip.info/Playlists/ASTRA_19_2E.m3u",
                                             @"http://www.satip.info/Playlists/ASTRA_28_2E.m3u",
                                             @"http://www.satip.info/Playlists/ASTRA_23_5E.m3u"];
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.satelliteListTableView reloadData];
    [super viewWillAppear:animated];
    [self.satelliteListTableView selectRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0] animated:NO scrollPosition:UITableViewScrollPositionNone];
}

- (IBAction)addServer:(id)sender
{
    NSLog(@"%s", __PRETTY_FUNCTION__);
}

- (IBAction)addChannelList:(id)sender
{
    NSLog(@"%s", __PRETTY_FUNCTION__);
}

#pragma mark - table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (tableView == self.satelliteListTableView) {
        return  _playlistTitlesToChooseFromArray.count;
    }

    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    SESTableViewCell *cell;
    if (tableView == self.serverListTableView) {
        cell = [self.serverListTableView dequeueReusableCellWithIdentifier:SESServerListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [SESTableViewCell new];
        }
        return cell;
    } else if (tableView == self.satelliteListTableView) {
        SESTableViewCell *cell = [self.satelliteListTableView dequeueReusableCellWithIdentifier:SESSatelliteListReUseIdentifier];
        if (!cell) {
            cell = [SESTableViewCell new];
        }
        cell.channelNameLabel.text = _playlistTitlesToChooseFromArray[indexPath.row];
        cell.channelIconImageView.image = nil;
        return cell;
    } else {
        cell = [self.channelListTableView dequeueReusableCellWithIdentifier:SESChannelListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [SESTableViewCell new];
        }
        return cell;
    }

    return cell;
}

#pragma mark - table view delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSLog(@"%s: %@", __PRETTY_FUNCTION__, indexPath);
}

@end
