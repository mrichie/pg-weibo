PhoneGap-weibo
========

Weibo Plugin for Phonegap

# How to Install
## IOS

* Add Plugin `cordova plugin add https://github.com/mrichie/pg-weibo.git`
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
In your phonegap project, add following code binding to your weibo login button
```
navigator.weibo.init(function(response) {
  navigator.weibo.login(function(token) {
    console.log(token);
  }, function(msg) {
    console.log("login error : " + msg);
  });
}, function(response) {
}, APP_ID, "https://api.weibo.com/oauth2/default.html");

```
