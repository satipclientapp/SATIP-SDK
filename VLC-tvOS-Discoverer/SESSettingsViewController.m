//
//  SESSettingsViewController.m
//  VLC-Discoverer
//
//  Created by Felix Paul Kühne on 12/07/16.
//  Copyright © 2016 VideoLabs SAS. All rights reserved.
//

#import "SESSettingsViewController.h"

NSString *SESServerListReUseIdentifier = @"SESServerListReUseIdentifier";
NSString *SESSatelliteListReUseIdentifier = @"SESSatelliteListReUseIdentifier";
NSString *SESChannelListReUseIdentifier = @"SESChannelListReUseIdentifier";

@interface SESSettingsViewController () <UITableViewDelegate, UITableViewDataSource>

@end

@implementation SESSettingsViewController

- (NSString *)title
{
    return @"Settings";
}

- (void)viewDidLoad {
    [super viewDidLoad];

    self.serverListTableView.dataSource = self;
    self.serverListTableView.delegate = self;
    [self.serverListTableView registerClass:[UITableViewCell class] forCellReuseIdentifier:SESServerListReUseIdentifier];

    self.satelliteListTableView.dataSource = self;
    self.satelliteListTableView.delegate = self;
    [self.satelliteListTableView registerClass:[UITableViewCell class] forCellReuseIdentifier:SESSatelliteListReUseIdentifier];

    self.channelListTableView.dataSource = self;
    [self.channelListTableView registerClass:[UITableViewCell class] forCellReuseIdentifier:SESChannelListReUseIdentifier];
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
    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell;
    if (tableView == self.serverListTableView) {
        cell = [self.serverListTableView dequeueReusableCellWithIdentifier:SESServerListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:SESServerListReUseIdentifier];
        }
    } else if (tableView == self.satelliteListTableView) {
        cell = [self.satelliteListTableView dequeueReusableCellWithIdentifier:SESSatelliteListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:SESSatelliteListReUseIdentifier];
        }
    } else {
        cell = [self.channelListTableView dequeueReusableCellWithIdentifier:SESChannelListReUseIdentifier forIndexPath:indexPath];
        if (cell == nil) {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:SESChannelListReUseIdentifier];
        }
    }

    return cell;
}

#pragma mark - table view delegation

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSLog(@"%s: %@", __PRETTY_FUNCTION__, indexPath);
}

@end
