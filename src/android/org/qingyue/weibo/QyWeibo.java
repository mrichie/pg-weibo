package org.qingyue.weibo;

import java.util.concurrent.ExecutorService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuth;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.*;
import com.sina.weibo.sdk.openapi.models.User;

public class QyWeibo extends CordovaPlugin {

    private static final String TAG = QyWeibo.class.getSimpleName();

    private static WeiboAuth mWeiboAuth;

    private Oauth2AccessToken mAccessToken;

    private SsoHandler mSsoHandler;

    private CallbackContext callbackContext;
    
    private UsersAPI mUsersAPI;
    
    public static JSONObject getObjectFromArray(JSONArray jsonArray,
                                                int objectIndex) {
        JSONObject jsonObject = null;
        if (jsonArray != null && jsonArray.length() > 0) {
            try {
                jsonObject = new JSONObject(jsonArray.get(objectIndex).toString());
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
	public boolean execute(String action, final JSONArray args, final CallbackContext cbContext) {
    	callbackContext = cbContext;
		boolean result = false;
        try {
            if (action.equals("init")) { 
				this.init(args, callbackContext);
                result = true;
            }
            else if(action.equals("login")){
            	this.login(args, callbackContext);
            	result = true;
            }
            else if(action.equals("getUserInfo")){
            	this.getUserInfo(args, callbackContext);
            	result = true;
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	callbackContext.error(getErrorObject(e.getMessage()));
			result = false;
        }
		return result;
    }

    public void init(JSONArray json, CallbackContext cbContext){
        String appId = getData(json, "appKey");
        String redirectURI = getData(json, "redirectURI");
        String scope = getData(json, "scope");
        if(scope == null){
        	scope = "all";
        }
        try{
        	mWeiboAuth = new WeiboAuth(this.cordova.getActivity().getApplicationContext(), appId, redirectURI, scope);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);
        }
        catch(Exception e){
            e.printStackTrace();
            callbackContext.error(getErrorObject(e.getMessage()));
        }
    }

    public void login(JSONArray json, CallbackContext cbContext){
        mSsoHandler = new SsoHandler(this.cordova.getActivity(), mWeiboAuth);
        this.cordova.setActivityResultCallback(this);
        mSsoHandler.authorize(new AuthDialogListener(QyWeibo.this));
    }
    
    public void getUserInfo(JSONArray json, CallbackContext cbContext){
    	// 获取当前已保存过的 Token
        mAccessToken = AccessTokenKeeper.readAccessToken(this.cordova.getActivity().getApplicationContext());
        // 获取用户信息接口
        mUsersAPI = new UsersAPI(mAccessToken);
    	long uid = Long.parseLong(mAccessToken.getUid());
    	mUsersAPI.show(uid, mListener);
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
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
				}  catch (JSONException e) {
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
                AccessTokenKeeper.writeAccessToken(fba.cordova.getActivity().getApplicationContext(), mAccessToken);
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

    private JSONObject getErrorObject(String message){
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

        private static final String KEY_UID           = "uid";
        private static final String KEY_ACCESS_TOKEN  = "access_token";
        private static final String KEY_EXPIRES_IN    = "expires_in";
        
        /**
         * 保存 Token 对象到 SharedPreferences。
         * 
         * @param context 应用程序上下文环境
         * @param token   Token 对象
         */
        public static void writeAccessToken(Context context, Oauth2AccessToken token) {
            if (null == context || null == token) {
                return;
            }
            
            SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_APPEND);
            Editor editor = pref.edit();
            editor.putString(KEY_UID, token.getUid());
            editor.putString(KEY_ACCESS_TOKEN, token.getToken());
            editor.putLong(KEY_EXPIRES_IN, token.getExpiresTime());
            editor.commit();
        }

        /**
         * 从 SharedPreferences 读取 Token 信息。
         * 
         * @param context 应用程序上下文环境
         * 
         * @return 返回 Token 对象
         */
        public static Oauth2AccessToken readAccessToken(Context context) {
            if (null == context) {
                return null;
            }
            
            Oauth2AccessToken token = new Oauth2AccessToken();
            SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_APPEND);
            token.setUid(pref.getString(KEY_UID, ""));
            token.setToken(pref.getString(KEY_ACCESS_TOKEN, ""));
            token.setExpiresTime(pref.getLong(KEY_EXPIRES_IN, 0));
            return token;
        }

        /**
         * 清空 SharedPreferences 中 Token信息。
         * 
         * @param context 应用程序上下文环境
         */
        public static void clear(Context context) {
            if (null == context) {
                return;
            }
            
            SharedPreferences pref = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_APPEND);
            Editor editor = pref.edit();
            editor.clear();
            editor.commit();
        }
    }

}
