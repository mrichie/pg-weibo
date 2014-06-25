//
//  QyWeibo.h
//
//  Created by Richie.Min on 6/20/14.
//
//

#import <Cordova/CDV.h>
#import "WeiboSDK.h"

@interface QyWeibo : CDVPlugin <WeiboSDKDelegate, WBHttpRequestDelegate>

- (void)init: (CDVInvokedUrlCommand* )command;
- (void)login: (CDVInvokedUrlCommand* )command;
- (void)getUserInfo: (CDVInvokedUrlCommand* )command;

@property (nonatomic, strong) CDVInvokedUrlCommand *pendingCommand;

@property NSString* appKey;
@property NSString* appSecret;
@property NSString* redirectURI;

@end
