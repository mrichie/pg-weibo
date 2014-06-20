package com.qingyue.weibo.QyWeibo;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

public class QyWeibo extends Plugin {

    private static final String TAG = QyWeibo.class.getSimpleName();

    private String callbackId;

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
            result = getObjectFromArray(ary).getString(key);
        } catch (JSONException e) {

        }
        return result;
    }

    @Override
    public PluginResult execute(String action, final JSONArray args,
                                final String callbackId) {
        final PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        Log.d(TAG, "action: " + action);

        if (action.equals("init")) {

            this.callbackId = callbackId;
            Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        final Weibo weibo = Weibo.getInstance();
                        try {
                            String appId = getData(args, 'appKey');
                            String appSecret = getData(args, 'appSecret');
                            String redirectURI = getData(args, 'redirectURI')
                            weibo.setupConsumerConfig(appId, appSecret);
                            weibo.setRedirectUrl(redirectURI);

                            QyWeibo.this.success("success",
                                                     QyWeibo.this.callbackId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            QyWeibo.this.error("error",
                                                   QyWeibo.this.callbackId);
                        }
                    }
                };
            this.cordova.getActivity().runOnUiThread(runnable);

        } else if (action.equals("login")) {
            final Weibo weibo = Weibo.getInstance();
            this.callbackId = callbackId;

            Runnable runnable = new Runnable() {
                    public void run() {
                        weibo.authorize(
                                        (Activity) QyWeibo.this.cordova.getActivity(),
                                        new AuthDialogListener(QyWeibo.this));
                    };
                };
            this.cordova.getActivity().runOnUiThread(runnable);

        }

        return pr;
    }

    class AuthDialogListener implements WeiboDialogListener {
        final QyWeibo fba;

        public AuthDialogListener(QyWeibo fba) {
            super();
            this.fba = fba;
        }

        @Override
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");
            // Log.d(TAG, "token: " + token + ", expires_in: " + expires_in);
            String json = "{\"access_token\": \"" + token
                + "\", \"expires_in\": \"" + expires_in + "\"}";
            JSONObject jo = null;
            try {
                jo = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            this.fba.success(jo, this.fba.callbackId);
        }

        @Override
        public void onError(DialogError e) {
            this.fba.error("Error: " + e.getMessage(), this.fba.callbackId);
        }

        @Override
        public void onCancel() {
            this.fba.error("Cancelled", this.fba.callbackId);
        }

        @Override
        public void onWeiboException(WeiboException e) {
            this.fba.error("Weibo error: " + e.getMessage(),
                           this.fba.callbackId);
        }
    }

}
