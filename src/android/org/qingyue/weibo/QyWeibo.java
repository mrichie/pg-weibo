package org.qingyue.weibo;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuth;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

public class QyWeibo extends CordovaPlugin {

    private static final String TAG = QyWeibo.class.getSimpleName();
    
    private static WeiboAuth mWeiboAuth;
    
    private Oauth2AccessToken mAccessToken;

    private SsoHandler mSsoHandler;

    private CallbackContext callbackContext;

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

    public void init(JSONArray json, CallbackContext cbContext){
    	callbackContext = cbContext;
    	String appId = getData(json, "app_key");
        String appSecret = getData(json, "appSecret");
        String redirectURI = getData(json, "redirectURI");
        try{
        	mWeiboAuth = new WeiboAuth(null, appId, appSecret, redirectURI);
         
        	PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        	pluginResult.setKeepCallback(true);
        	callbackContext.sendPluginResult(pluginResult);
        }
        catch(Exception e){
        	e.printStackTrace();
        	PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    }
    
    public void login(JSONArray json, CallbackContext callbackContext){
    	Runnable runnable = new Runnable() {
            public void run() {
            	mSsoHandler = new SsoHandler((Activity) QyWeibo.this.cordova.getActivity(), mWeiboAuth);
            	mSsoHandler.authorize(new AuthDialogListener(QyWeibo.this));
            	
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    	
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }
    
    class AuthDialogListener implements WeiboAuthListener {
        final QyWeibo fba;

        public AuthDialogListener(QyWeibo fba) {
            super();
            this.fba = fba;
        }

        public void onComplete(Bundle values) {
        	mAccessToken = Oauth2AccessToken.parseAccessToken(values);
        	if (mAccessToken.isSessionValid()) {
                // save Token to SharedPreferences
                //AccessTokenKeeper.writeAccessToken(QyWeibo.this, mAccessToken);
            } else {
                String code = values.getString("code");
                Log.d(TAG, "code : " + code);
            }
        	
            String token = mAccessToken.getToken();
            String expires_in = Long.toString((mAccessToken.getExpiresTime()));
            Log.d(TAG, "token: " + token + ", expires_in: " + expires_in);
            String json = "{\"access_token\": \"" + token
                + "\", \"expires_in\": \"" + expires_in + "\"}";
            JSONObject jo = null;
            try {
                jo = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            this.fba.callbackContext.success(jo);
        }

        @Override
        public void onCancel() {
            this.fba.callbackContext.error("Cancelled");
        }

        @Override
        public void onWeiboException(WeiboException e) {
            this.fba.callbackContext.error("Weibo error: " + e.getMessage());
        }

    }

}
