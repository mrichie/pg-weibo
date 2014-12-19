PhoneGap-weibo
========

Weibo Plugin for Phonegap
#分享图片和文字#
```
		navigator.weibo.shareMessage({type: 'image', data: path, text: text}, function(){
		    console.log('send success');
		}, function(err){
		    console.log("Err: ");
		    console.log(err);
		});
```
