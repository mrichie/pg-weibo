package org.qingyue.weibo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.phonegap.plugins.weixin.WeiXin;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler.Response;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.UsersAPI;

public class QyWeibo extends CordovaPlugin {

	private static final String TAG = QyWeibo.class.getSimpleName();

	private String appKey;

	// private static WeiboAuth mWeiboAuth;
	private AuthInfo mAuthInfo;

	private Oauth2AccessToken mAccessToken;

	private SsoHandler mSsoHandler;

	private CallbackContext callbackContext;

	private UsersAPI mUsersAPI;

	// private IWeiboShareAPI mWeiboShareAPI;
	/** 微博微博分享接口实例 */
	private IWeiboShareAPI mWeiboShareAPI = null;

	public static JSONObject getObjectFromArray(JSONArray jsonArray,
			int objectIndex) {
		JSONObject jsonObject = null;
		if (jsonArray != null && jsonArray.length() > 0) {
			try {
				jsonObject = new JSONObject(jsonArray.get(objectIndex)
						.toString());
			} catch (JSONException e) {

			}
		}
		return jsonObject;
	}

	public static String getData(JSONArray ary, String key) {
		String result = null;
		try {
			result = getObjectFromArray(ary, 0).getString(key);
		} catch (JSONException e) {

		}
		return result;
	}

	@Override
	public boolean execute(String action, final JSONArray args,
			final CallbackContext cbContext) {
		callbackContext = cbContext;
		boolean result = false;
		try {
			if (action.equals("init")) {
				this.init(args, callbackContext);
				result = true;
			} else if (action.equals("login")) {
				this.login(args, callbackContext);
				result = true;
			} else if (action.equals("getUserInfo")) {
				this.getUserInfo(args, callbackContext);
				result = true;
			} else if (action.equals("shareMessage")) {
				final QyWeibo me = this;
				cordova.getThreadPool().execute(new Runnable() {
					public void run() {
						try {
							JSONObject cfg = args.getJSONObject(0);
							if (cfg.getString("type").equals("text")) {
								me.sendText(cfg.getString("text"));
							} else if (cfg.getString("type").equals("image")) {
								me.sendImage(cfg.getString("data"),
										cfg.getString("text"));
							}
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							callbackContext.error("JSON Exception");
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							callbackContext.error("JSON Exception");
							e.printStackTrace();
						} catch (JSONException e) {
							callbackContext.error("JSON Exception");
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			callbackContext.error(getErrorObject(e.getMessage()));
			result = false;
		}
		return result;
	}

	public void init(JSONArray json, CallbackContext cbContext) {
		appKey = getData(json, "appKey");
		String redirectURI = getData(json, "redirectURI");
		String scope = getData(json, "scope");
		if (scope == null) {
			scope = "all";
		}
		try {
			mAuthInfo = new AuthInfo(this.cordova.getActivity()
					.getApplicationContext(), appKey, redirectURI, scope);
			// mSsoHandler = new SsoHandler(WBAuthActivity.this, mAuthInfo);

			// mWeiboAuth = new
			// WeiboAuth(this.cordova.getActivity().getApplicationContext(),
			// appKey, redirectURI, scope);
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
			callbackContext.sendPluginResult(pluginResult);
		} catch (Exception e) {
			e.printStackTrace();
			callbackContext.error(getErrorObject(e.getMessage()));
		}
	}

	public void login(JSONArray json, CallbackContext cbContext) {
		mSsoHandler = new SsoHandler(this.cordova.getActivity(), mAuthInfo);
		this.cordova.setActivityResultCallback(this);
		// mSsoHandler.authorize(new AuthDialogListener(QyWeibo.this));
		mSsoHandler
				.authorizeClientSso((WeiboAuthListener) new AuthDialogListener(
						QyWeibo.this));
	}

	public void getUserInfo(JSONArray json, CallbackContext cbContext) {
		// 获取当前已保存过的 Token
		mAccessToken = AccessTokenKeeper.readAccessToken(this.cordova
				.getActivity().getApplicationContext());
		// 获取用户信息接口
		mUsersAPI = new UsersAPI(this.cordova.getActivity()
				.getApplicationContext(), appKey, mAccessToken);
		long uid = Long.parseLong(mAccessToken.getUid());
		mUsersAPI.show(uid, mListener);
	}

	public void sendText(String text) throws MalformedURLException, IOException {
		// String text = getData(json, "text");
		// String image_path = getData(json, "image");
		Log.d(TAG, "app key : " + appKey);
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(
				this.cordova.getActivity(), appKey);
		mWeiboShareAPI.registerApp(); // 将应用注册到微博客户端
		WeiboMultiMessage weiboMessage = new WeiboMultiMessage();// 初始化微博的分享消息
		if (text != null) {
			TextObject textObject = new TextObject();
			textObject.text = text;
			weiboMessage.textObject = textObject;
		}
		SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
		request.transaction = String.valueOf(System.currentTimeMillis());
		request.multiMessage = weiboMessage;
		mWeiboShareAPI.sendRequest(this.cordova.getActivity(), request); // 发送请求消息到微博,唤起微博分享界面
	}

	public void sendImage(String data, String text)
			throws MalformedURLException, IOException {
		// String text = getData(json, "text");
		String image_path = data;
		Log.d(TAG, "app key : " + appKey);
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(
				this.cordova.getActivity(), appKey);
		mWeiboShareAPI.registerApp(); // 将应用注册到微博客户端
		WeiboMultiMessage weiboMessage = new WeiboMultiMessage();// 初始化微博的分享消息
		if (text != null) {
			TextObject textObject = new TextObject();
			textObject.text = text;
			weiboMessage.textObject = textObject;
		}
		if (image_path != null) {
			ImageObject imageObject = new ImageObject();
			if (image_path.startsWith("data")) {
				String dataUrl = image_path;
				String encodingPrefix = "base64,";
				int contentStartIndex = dataUrl.indexOf(encodingPrefix)
						+ encodingPrefix.length();
				String resData = dataUrl.substring(contentStartIndex);

				byte[] bytes = null;
				try {
					bytes = Base64.decode(resData, 0);
				} catch (Exception ignored) {
					Log.e("Weixin", "Invalid Base64 string");
				}
				imageObject.imageData = bytes;
			} else if (image_path.startsWith("http://")) {
				Bitmap bmp = null;
				bmp = BitmapFactory.decodeStream(new URL(image_path)
						.openStream());
				imageObject.setImageObject(bmp);
			} else {
				imageObject.imagePath = image_path;
			}
			weiboMessage.imageObject = imageObject;
		}
		SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
		request.transaction = String.valueOf(System.currentTimeMillis());
		request.multiMessage = weiboMessage;
		mWeiboShareAPI.sendRequest(this.cordova.getActivity(), request); // 发送请求消息到微博,唤起微博分享界面
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mSsoHandler != null) {
			mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// 从当前应用唤起微博并进行分享后，返回到当前应用时，需要在此处调用该函数
		// 来接收微博客户端返回的数据；执行成功，返回 true，并调用
		// {@link IWeiboHandler.Response#onResponse}；失败返回 false，不调用上述回调
		mWeiboShareAPI.handleWeiboResponse(intent, (Response) this);
	}

	public void onResponse(BaseResponse baseResp) {
		// 接收微客户端博请求的数据。
		Log.d(TAG, "  " + baseResp.errMsg);
		switch (baseResp.errCode) {
		case WBConstants.ErrorCode.ERR_OK:

			break;
		case WBConstants.ErrorCode.ERR_CANCEL:

			break;
		case WBConstants.ErrorCode.ERR_FAIL:

			break;
		}
	}

	/**
	 * 微博 OpenAPI 回调接口。
	 */
	private RequestListener mListener = new RequestListener() {
		@Override
		public void onComplete(String response) {
			if (!TextUtils.isEmpty(response)) {
				JSONObject jsonObj;
				try {
					jsonObj = new JSONObject(response);
					callbackContext.success(jsonObj);
				} catch (JSONException e) {
					e.printStackTrace();
					callbackContext.error(getErrorObject(e.getMessage()));
				}
			}
		}

		@Override
		public void onWeiboException(WeiboException e) {
			callbackContext.error(getErrorObject(e.getMessage()));
		}
	};

	class AuthDialogListener implements WeiboAuthListener {
		QyWeibo fba;

		public AuthDialogListener(QyWeibo fba) {
			super();
			this.fba = fba;
		}

		public void onComplete(Bundle values) {
			mAccessToken = Oauth2AccessToken.parseAccessToken(values);
			if (mAccessToken.isSessionValid()) {
				// save Token to SharedPreferences
				AccessTokenKeeper.writeAccessToken(fba.cordova.getActivity()
						.getApplicationContext(), mAccessToken);
			} else {
				String code = values.getString("code");
				Log.d(TAG, "code : " + code);
			}

			String token = mAccessToken.getToken();
			String expires_in = Long.toString((mAccessToken.getExpiresTime()));

			JSONObject json = new JSONObject();
			try {
				json.put("token", token);
				json.put("expires_in", expires_in);
				callbackContext.success(json);
			} catch (JSONException e) {
				e.printStackTrace();
				callbackContext.error(getErrorObject(e.getMessage()));
			}
		}

		@Override
		public void onCancel() {
			callbackContext.error("Cancelled");
		}

		@Override
		public void onWeiboException(WeiboException e) {
			callbackContext.error(getErrorObject(e.getMessage()));
		}
	}

	private JSONObject getErrorObject(String message) {
		JSONObject json = new JSONObject();
		try {
			json.put("error", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	/**
	 * 该类定义了微博授权时所需要的参数。
	 * 
	 * @author SINA
	 * @since 2013-10-07
	 */
	public static class AccessTokenKeeper {
		private static final String PREFERENCES_NAME = "com_weibo_sdk_android";

		private static final String KEY_UID = "uid";
		private static final String KEY_ACCESS_TOKEN = "access_token";
		private static final String KEY_EXPIRES_IN = "expires_in";

		/**
		 * 保存 Token 对象到 SharedPreferences。
		 * 
		 * @param context
		 *            应用程序上下文环境
		 * @param token
		 *            Token 对象
		 */
		public static void writeAccessToken(Context context,
				Oauth2AccessToken token) {
			if (null == context || null == token) {
				return;
			}

			SharedPreferences pref = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_APPEND);
			Editor editor = pref.edit();
			editor.putString(KEY_UID, token.getUid());
			editor.putString(KEY_ACCESS_TOKEN, token.getToken());
			editor.putLong(KEY_EXPIRES_IN, token.getExpiresTime());
			editor.commit();
		}

		/**
		 * 从 SharedPreferences 读取 Token 信息。
		 * 
		 * @param context
		 *            应用程序上下文环境
		 * 
		 * @return 返回 Token 对象
		 */
		public static Oauth2AccessToken readAccessToken(Context context) {
			if (null == context) {
				return null;
			}

			Oauth2AccessToken token = new Oauth2AccessToken();
			SharedPreferences pref = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_APPEND);
			token.setUid(pref.getString(KEY_UID, ""));
			token.setToken(pref.getString(KEY_ACCESS_TOKEN, ""));
			token.setExpiresTime(pref.getLong(KEY_EXPIRES_IN, 0));
			return token;
		}

		/**
		 * 清空 SharedPreferences 中 Token信息。
		 * 
		 * @param context
		 *            应用程序上下文环境
		 */
		public static void clear(Context context) {
			if (null == context) {
				return;
			}

			SharedPreferences pref = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_APPEND);
			Editor editor = pref.edit();
			editor.clear();
			editor.commit();
		}
	}

}
