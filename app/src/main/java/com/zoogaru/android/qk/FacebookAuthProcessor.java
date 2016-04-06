package com.zoogaru.android.qk;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by sandeep on 4/6/16.
 */
public class FacebookAuthProcessor implements FacebookCallback<LoginResult> {

    private static final String TAG = "Quickoo";

    INotifyUs m_notifyListener ;
    CallbackManager m_callbackManager;
    AccessTokenTracker m_accessTokenTracker;
    ProfileTracker m_profileTracker;
    AccessToken m_accessToken;

    FacebookAuthProcessor(INotifyUs aListener, Context aContext)
    {
        m_notifyListener = aListener;
        FacebookSdk.sdkInitialize(aContext);
        m_callbackManager = CallbackManager.Factory.create();
        m_accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                m_accessToken = currentAccessToken;
                // currentAccessToken when it's loaded or set.
            }
        };

        m_profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(
                    Profile oldProfile,
                    Profile currentProfile) {
                if(currentProfile != null) {
                    getFacebookData(currentProfile);
                    Log.d(TAG, "New profile info available");
                }
            }
        };
    }

    void getCurrentProfile()
    {
        Profile.fetchProfileForCurrentAccessToken();
        Profile profile = Profile.getCurrentProfile();
        if(profile != null)
        {
            getFacebookData(profile);
        }
    }

    private Bundle getFacebookData(Profile object) {

        Bundle bundle = null;
        try {
            bundle = new Bundle();
            String id = object.getId();
            URL profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=50&height=50");
            Log.i("profile_pic", profile_pic + "");
            bundle.putString("profile_pic", profile_pic.toString());
            bundle.putString("idFacebook", id);
            bundle.putString("name", object.getName());
            return bundle;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return bundle;
    }

    void stopTrackingChanges()
    {
        m_accessTokenTracker.stopTracking();
        m_profileTracker.stopTracking();
    }

    CallbackManager getCallbackManager()
    {
        return m_callbackManager;
    }

    private Bundle getFacebookData(JSONObject object) {

        Bundle bundle = null;
        try {
            bundle = new Bundle();
            String id = object.getString("id");
            URL profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=50&height=50");
            Log.i("profile_pic", profile_pic + "");
            bundle.putString("profile_pic", profile_pic.toString());
            String name = "";
            bundle.putString("idFacebook", id);
            if (object.has("first_name"))
                name = object.getString("first_name");
            if (object.has("last_name"))
                name = name + " "  + object.getString("last_name");
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));
            if (object.has("gender"))
                bundle.putString("gender", object.getString("gender"));
            if (object.has("birthday"))
                bundle.putString("birthday", object.getString("birthday"));
            if (object.has("location"))
                bundle.putString("location", object.getJSONObject("location").getString("name"));

            bundle.putString("name", name);
            return bundle;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return bundle;
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {

                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            Log.i("LoginActivity", response.toString());
                            // Get facebook data from login
                            Bundle bFacebookData = getFacebookData(object);
                            m_notifyListener.notifyus("FacebookDataAvailable", bFacebookData);
                        }
                    });

            Bundle parameters = new Bundle();
            parameters.putString("fields", "id, first_name, last_name, email,gender, birthday, location");
            request.setParameters(parameters);
            request.executeAsync();
    }

    @Override
    public void onCancel() {
        m_notifyListener.notifyus("FacebookAuthCancelled", null);
    }

    @Override
    public void onError(FacebookException exception) {
        m_notifyListener.notifyus("FacebookAuthError", null);
    }
}
