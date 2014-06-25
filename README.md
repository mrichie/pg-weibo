PhoneGap-weibo
========

Weibo Plugin for Phonegap

# How to Install
## IOS

* `cordova plugin add https://github.com/mrichie/pg-weibo.git`
* Add "wb[YOUR_APP_KEY]" to `URL Schemas` under `URL types` into `Info.plist`
* Add following code to `AppDelegate.m`
```
#import "WeiboSDK.h"
#import "QyWeibo.h"

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation
{
    QyWeibo *weiboPlugin = [self.viewController.pluginObjects objectForKey:@"QyWeibo"];
    return [WeiboSDK handleOpenURL:url delegate:weiboPlugin];
}

```

# How to Use
In your phonegap project, add following code to your weibo login button
```
            navigator.weibo.init(function(response) {
                console.log('response : ' + response);
                navigator.weibo.login(function(access_token, expires_in) {
                    console.log(access_token);
                }, function(msg) {
                    console.log("login error : " + msg);
                });
            }, function(response) {
            }, APP_ID, APP_SECRET, "https://api.weibo.com/oauth2/default.html");

```
