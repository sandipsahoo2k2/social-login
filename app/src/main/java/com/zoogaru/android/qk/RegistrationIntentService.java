package com.zoogaru.android.qk;

/**
 * Created by sandeep on 3/31/16.
 */

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }
    AtomicInteger msgId = new AtomicInteger();

    @Override
    protected void onHandleIntent(Intent intent) {
            sendToServer(intent);
    }

    private void sendToServer(Intent aClientIntent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String msg = "";
        String action = aClientIntent.getAction().toString();
        try
        {
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            Bundle clientData = aClientIntent.getExtras();
            switch(action)
            {
                case "SIGNUP":
                    InstanceID instanceID = InstanceID.getInstance(this);
                    String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    clientData.putString("action", "SIGNUP");
                    msg = "Sent Signup message";
                    subscribeTopics(token);
                    break;
                default:
                    break;
            }
            String id = Integer.toString(msgId.incrementAndGet());
            gcm.send(getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com", id, QKPreferences.GCM_TIME_TO_LIVE, clientData);
            if(action.equals("SIGNUP"))
            {
                sharedPreferences.edit().putBoolean(QKPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                // Notify UI that registration has completed, so the progress indicator can be hidden.
                Intent registrationComplete = new Intent(QKPreferences.REGISTRATION_COMPLETE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
            }
        } catch (Exception e) {
            msg = "Error :" + e.getMessage();
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            if(action.equals("SIGNUP"))
            {
                Log.d(TAG, "Failed to complete token refresh", e);
                sharedPreferences.edit().putBoolean(QKPreferences.SENT_TOKEN_TO_SERVER, false).apply();
            }
        }
        Log.d(TAG, msg);
    }

    private void subscribeTopics(String token) throws IOException {
        Log.i(TAG, "GCM Registration Token: " + token);
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
