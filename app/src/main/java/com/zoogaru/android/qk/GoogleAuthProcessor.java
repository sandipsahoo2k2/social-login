package com.zoogaru.android.qk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.Account;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;

import java.net.URL;
import java.util.List;


/**
 * Created by sandeep on 4/5/16.
 */
public class GoogleAuthProcessor implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult> {

    private static final String TAG = "Quickoo";
    private static final int RC_SIGN_IN = 9001;
    INotifyUs m_notifyListener ;
    GoogleApiClient m_GoogleApiClient;
    AppCompatActivity m_activity ;
    boolean m_isGPlusCall ;

    GoogleAuthProcessor(INotifyUs aListener, AppCompatActivity aContext)
    {
        m_notifyListener = aListener;
        m_activity = aContext ;
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(m_GoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }


    public void signIn(boolean isGplus)
    {
        m_isGPlusCall = isGplus;
        if(m_isGPlusCall)
        {
            m_GoogleApiClient = new GoogleApiClient.Builder(m_activity)
                    .enableAutoManage(m_activity, this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .build();

            m_GoogleApiClient.connect();
            m_notifyListener.notifyus("gPlusAuthStarted", null);
        }
        else
        {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail().requestProfile()
                    .build();

            m_GoogleApiClient = new GoogleApiClient.Builder(m_activity)
                    .enableAutoManage(m_activity, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(m_GoogleApiClient);
            m_activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            m_notifyListener.notifyus("gAuthStarted", signInIntent);
        }
    }

    public Bundle getGoogleAccData(GoogleSignInAccount googleAccont) {
        Bundle bundle = null;
        bundle = new Bundle();
        String id = googleAccont.getId();
        Uri phtoUrl = googleAccont.getPhotoUrl();
        if(phtoUrl != null) {
            Log.i("profile_pic: ", phtoUrl.toString());
            bundle.putString("profile_pic", phtoUrl.toString());
        }
        bundle.putString("idGPlus", id);
        bundle.putString("name", googleAccont.getDisplayName());
        bundle.putString("email", googleAccont.getEmail());
        return bundle;
    }

    private Bundle getGooglePlusData(Person gPlusAccount) {
        Bundle bundle = null;
        bundle = new Bundle();
        int gender = gPlusAccount.getGender();
        String id = gPlusAccount.getId();
        Person.Image photo = gPlusAccount.getImage();
        if(photo != null) {
            Log.i("profile_pic: ", photo.getUrl());
            bundle.putString("profile_pic", photo.getUrl());
        }
        bundle.putString("idGPlus", id);
        bundle.putInt("gender", gender);
        bundle.putString("name", gPlusAccount.getDisplayName());
        return bundle;
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Bundle gPlusData = getGoogleAccData(acct);
            m_notifyListener.notifyus("googleAuthDataAvailable", gPlusData);
        } else {
            m_notifyListener.notifyus("googleAuthFailed", null);
            // Signed out, show unauthentic
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Get user's information
        Log.d(TAG, "onConnected called");
        if(bundle != null) {
            Log.d(TAG,  bundle.toString());
        }
        Plus.PeopleApi.load(m_GoogleApiClient, "me").setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        m_notifyListener.notifyus("gPlusConnectionFailed", null);
    }

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {
        Log.d(TAG, "onResult called");
        if (loadPeopleResult.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS)
        {
            PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
            try
            {
                int count = personBuffer.getCount();
                if (count == 1)
                {
                    Person person = personBuffer.get(0);
                    Bundle data = getGooglePlusData(person);
                    m_notifyListener.notifyus("gPlusAuthDataAvailable", data);
                }
            }
            finally
            {
                personBuffer.release();
            }
        }
        else
        {
            m_notifyListener.notifyus("gPlusAuthNoData", null);
            Log.e(TAG, "Error requesting gplus profile: " + loadPeopleResult.getStatus());
        }
    }
}
