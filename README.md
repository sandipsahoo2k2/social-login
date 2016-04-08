
# Social-Login
An android app example that does facebook or google or G+ login and fetch user profile

#Prerequesite
Copy google-services.json file to app folder (Enable g+ login api from developer console and create a configuration file)
update the ApplicationId in application menifest file (Create a facebook app for FB Deveoper and get the ID) 

Compile the project in android studio and you are done.

#Motivation
While working on an idea of my new app I had this requirement of allowing users to use there Facebook or Google account. Facebook auth was easy and without any difficulty I could get the users profile info etc. But Google Auth + GPlus data fetching was a pain as they both involve different api strategy and I started working on it but I was stuck for days ! Unfortunately any example I found in internet for Google Auth + GPlus data fetching is either obsolete or doesnt work ! Even googles developer page is also not updated with correct functions. 

*It was quite impossible to get both data in one api call !* I then cracked it with the below minor changes to my existing code. Hope it helps to any android enthusiatic. 

#Fetch G+ data with GoogleSignin
A slight modification to GoogleSignInClient can fetchus both Google account + GPlus data

Create the GoogleSignInOptions as below (in GoogleAuthProcessor.java file)

    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope(Scopes.PLUS_LOGIN))
                    .build();
                    
Create the GoogleApiClient as below

    m_GoogleApiClient = new GoogleApiClient.Builder(m_activity)
                    .enableAutoManage(m_activity, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addApi(Plus.API)
                    .build();
                    
Add a new public function 

    public void fetchConnectedProfileInfo()
    {
        Log.d(TAG, "fetchConnectedProfileInfo");
        if (m_GoogleApiClient.hasConnectedApi(Plus.API)) {
            Plus.PeopleApi.load(m_GoogleApiClient, "me").setResultCallback(this);
        }
    }
    
    Call fetchConnectedProfileInfo() from MainActivity.java onActivityResult()

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        m_fbAuthProcessor.getCallbackManager().onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "handleSignInResult:" + result.isSuccess());
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                Bundle googleData = m_gAuthProcessor.getGoogleAccData(acct);
                notifyus("googleAuthDataAvailable", googleData);
                m_gAuthProcessor.fetchConnectedProfileInfo();
            } else {
                notifyus("googleAuthFailed", null);
                // Signed out, show unauthentic
            }
        }
    }

