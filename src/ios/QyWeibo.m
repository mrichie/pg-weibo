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
    
    NSString *scope = [self parseStringFromJS:command.arguments keyFromJS:@"scope"];
    if ([scope length] > 0){
        scope = @"all";
    }
    // send request
    WBAuthorizeRequest *request = [WBAuthorizeRequest request];
    request.redirectURI = self.redirectURI;
    request.scope = scope;
    [WeiboSDK sendRequest:request];
    self.pendingCommand = command;
}

- (void)getUserInfo:(CDVInvokedUrlCommand*) command{
    NSDictionary *token = [self loadToken];
    NSString *access_token = [token valueForKey:@"accessToken"];
    NSDictionary *params = @{
                             @"uid": [token valueForKey:@"userID"]
                             };
    
    NSString * userInfoUrl = @"https://api.weibo.com/2/users/show.json";
    
    [WBHttpRequest requestWithAccessToken:access_token url:userInfoUrl httpMethod:@"GET" params:params delegate:self withTag:@"userInfo"];
    
    self.pendingCommand = command;
}

- (void)shareMessage:(CDVInvokedUrlCommand*) command{
    self.pendingCommand = command;
    NSString *type = [self parseStringFromJS:command.arguments keyFromJS:@"type"];
    if([type  isEqual: @"text"]){
        [self sendTextContent];
    }else if([type  isEqual: @"image"]){
        [self sendImageContent];
    }
}

-(void) sendTextContent{
    NSString *text = [self parseStringFromJS:self.pendingCommand.arguments keyFromJS:@"text"];
    WBMessageObject *message = [WBMessageObject message];
    if([text length] > 0){
        message.text = text;
    }
    WBSendMessageToWeiboRequest *request = [WBSendMessageToWeiboRequest requestWithMessage:message];
    [WeiboSDK enableDebugMode:YES];
    [WeiboSDK sendRequest:request];
}

- (void)sendImageContent{
    NSString *text = [self parseStringFromJS:self.pendingCommand.arguments keyFromJS:@"text"];
    NSString *image_path = [self parseStringFromJS:self.pendingCommand.arguments keyFromJS:@"data"];
    WBMessageObject *message = [WBMessageObject message];
    if([text length] > 0){
        message.text = text;
    }
    if([image_path length] > 0){
        WBImageObject *wbimage = [WBImageObject object];
        NSData *image;
        if([image_path hasPrefix:@"http://"]){
            image = [NSData dataWithContentsOfURL: [NSURL URLWithString:image_path]];
        }
        else if([image_path hasPrefix:@"data:"]){
            image = [NSData dataWithContentsOfURL:[NSURL URLWithString:image_path]];
        }else{
            image = [NSData dataWithContentsOfFile: image_path];
        }
        wbimage.imageData = image;
        message.imageObject = wbimage;
    }
    WBSendMessageToWeiboRequest *request = [WBSendMessageToWeiboRequest requestWithMessage:message];
    [WeiboSDK enableDebugMode:YES];
    [WeiboSDK sendRequest:request];
}

- (void)didReceiveWeiboRequest:(WBBaseRequest *)request
{
    // do nothing
}

- (void)didReceiveWeiboResponse:(WBBaseResponse *)response
{
    NSLog(@"response : %@", response);
    if ([response isKindOfClass:[WBSendMessageToWeiboResponse class]])
    {
        NSLog(@"Return from send message: %ld", response.statusCode);
    }
    else if ([response isKindOfClass:[WBAuthorizeResponse class]])
    {
        WBAuthorizeResponse *authResponse = (WBAuthorizeResponse *)response;
        NSLog(@"%@", authResponse.userInfo);
        if([authResponse.userInfo valueForKey:@"error"]){
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[authResponse.userInfo valueForKey:@"error"]];
            
            [self.commandDelegate sendPluginResult:result
                                        callbackId:self.pendingCommand.callbackId];
        }else if ([authResponse.userInfo valueForKey:@"user_cancelled"]){
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            
            [self.commandDelegate sendPluginResult:result callbackId:self.pendingCommand.callbackId];
        }else{
            [self saveToken:authResponse];
            NSDictionary *dict = @{
                                   @"access_token": authResponse.accessToken,
                                   @"expires_in": authResponse.expirationDate.description
                                   };
            // send back
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
            
            [self.commandDelegate sendPluginResult:result
                                        callbackId:self.pendingCommand.callbackId];
            
            // clean up
            self.pendingCommand = nil;
        }
    }
    
}

- (void)request:(WBHttpRequest *)request didFinishLoadingWithDataResult:(NSData *)data
{
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data
                                                         options:NSJSONReadingMutableLeaves
                                                           error:nil];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
    
    [self.commandDelegate sendPluginResult:result
                                callbackId:self.pendingCommand.callbackId];
    
}

- (void)request:(WBHttpRequest *)request didFailWithError:(NSError *)error{
    NSLog(@"error : %@", error);
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
    
    [self.commandDelegate sendPluginResult:result
                                callbackId:self.pendingCommand.callbackId];
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
    if (commandArguments && commandArguments.count > 0) {
        return TRUE;
    }else{
        return FALSE;
    }
}

- (NSString*)parseStringFromJS:(NSArray*)commandArguments keyFromJS:(NSString*)key{
    if([self existCommandArguments:commandArguments]){
        NSString *value = [[commandArguments objectAtIndex:0] valueForKey:key];
        if(value){
            return [NSString stringWithFormat:@"%@",value];
        }else{
            return @"";
        }
    }else{
        return @"";
    }
}


@end
