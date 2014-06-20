//
//  QyWeibo.m
//
//  Created by Richie.Min on 6/20/14.
//
//

#import "QyWeibo.h"

#define kCDVWeiboDefaultKey @"CDVWeiboDefaultKey"

@implementation QyWeibo

- (void)init:(CDVInvokedUrlCommand* )command
{
    CDVPluginResult *result;
    NSString *message;
    self.appKey = [self parseStringFromJS:command.arguments keyFromJS:@"appKey"];
    self.appSecret = [self parseStringFromJS:command.arguments keyFromJS:@"appSecret"];
    self.redirectURI = [self parseStringFromJS:command.arguments keyFromJS:@"redirectURI"];

    if (!self.appKey)
    {
        message = @"App key was null.";
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return ;
    }

    [WeiboSDK registerApp:self.appKey];

    NSDictionary *token = [self loadToken];
    if (token)
    {
        message = [token objectForKey:@"accessToken"];
    }
    else
    {
        message = @"";
    }

    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)login:(CDVInvokedUrlCommand* )command
{
    CDVPluginResult *result;

    NSString *scope;
    if ([command.arguments count] > 1)
    {
        scope = [command.arguments objectAtIndex:1];
    }
    else
    {
        scope = @"all";
    }

    // send request
    WBAuthorizeRequest *request = [WBAuthorizeRequest request];
    request.redirectURI = self.redirectURI;
    request.scope = scope;
    [WeiboSDK sendRequest:request];

    self.pendingLoginCommand = command;
}

- (void)didReceiveWeiboRequest:(WBBaseRequest *)request
{
    // do nothing
}

- (void)didReceiveWeiboResponse:(WBBaseResponse *)response
{
    if ([response isKindOfClass:[WBSendMessageToWeiboResponse class]])
    {
        NSLog(@"Return from send message: %d", response.statusCode);
    }
    else if ([response isKindOfClass:[WBAuthorizeResponse class]])
    {
        WBAuthorizeResponse *authResponse = (WBAuthorizeResponse *)response;
        [self saveToken:authResponse];

        // send back
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:authResponse.accessToken];

        [self.commandDelegate sendPluginResult:result
                                    callbackId:self.pendingLoginCommand.callbackId];

        // clean up
        self.pendingLoginCommand = nil;
    }
}

- (NSDictionary *)loadToken
{
    NSDictionary *token = [[NSUserDefaults standardUserDefaults] objectForKey:kCDVWeiboDefaultKey];
    if (token)
    {
        // check expiration
        NSDate *expirationDate = [token objectForKey:@"expirationDate"];
        if([[NSDate date] timeIntervalSinceDate:expirationDate] > 0)
        {
            NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
            [defaults setObject:nil forKey:kCDVWeiboDefaultKey];
            [defaults synchronize];

            return nil;
        }
    }
    return token;
}

- (void)saveToken:(WBAuthorizeResponse *)response
{
    NSDictionary *token = @{
        @"userID": response.userID,
        @"accessToken": response.accessToken,
        @"expirationDate": response.expirationDate
    };

    // save token to user defaults
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:token forKey:kCDVWeiboDefaultKey];
    [defaults synchronize];
}

# pragma mark -
# pragma mark MISC
# pragma mark -

- (BOOL)existCommandArguments:(NSArray*)comArguments{
    NSMutableArray *commandArguments=[[NSMutableArray alloc] initWithArray:comArguments];
    if (commandArguments.count > 0) {
        return TRUE;
    }else{
        return FALSE;
    }
}

- (NSString*)parseStringFromJS:(NSArray*)commandArguments keyFromJS:(NSString*)key{
    NSString *string = [NSString stringWithFormat:@"%@",[[commandArguments objectAtIndex:0] valueForKey:key]];
    return string;
}


@end
