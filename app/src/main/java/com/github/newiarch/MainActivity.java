package com.github.newiarch;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class MainActivity extends Activity {

    final static private String APP_KEY = "fapxgsf7glvwkb0";
    final static private String APP_SECRET = "1swwbsarfhraqab";
    static DropboxAPI<AndroidAuthSession> mDBApi;
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    static Button mLinkButton;
    static String[] toBeUploaded = new String[0];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MainActivityFragment())
                    .commit();
        }

        //Create a new AuthSession so that we can use the Dropbox API
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI< >(session);
    }

    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mDBApi.getSession();
        mLinkButton = (Button) findViewById(R.id.link_button);

        if (session.authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                session.finishAuthentication();
                storeAuth(session);
                mLinkButton.setText("Unlink from Dropbox");
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String key = sharedPref.getString(ACCESS_KEY_NAME, null);
        String secret = sharedPref.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;
        session.setOAuth2AccessToken(secret);
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String accessToken = mDBApi.getSession().getOAuth2AccessToken();
        if (accessToken != null) {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(ACCESS_KEY_NAME, "oauth2:");
            editor.putString(ACCESS_SECRET_NAME, accessToken);
            editor.commit();
        }
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    public void takePicture(View view)
    {
        if (mDBApi.getSession().isLinked()) {
            // Create new fragment and transaction
            Fragment newFragment = new TakePictureFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack
            transaction.replace(R.id.container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
        else {
            Toast.makeText(MainActivity.this, "Error : Not connected to Dropbox",
                    Toast.LENGTH_LONG).show();
        }
    }

    //create a new project for TakePictureFragment project name dropdown
    public void newProject(View v) {
       	//TakePictureFragment.newProject(v);
       	AddDialogHandler dialogHandler = new AddDialogHandler();
    	dialogHandler.show(this.getFragmentManager(), "addImage");
    }

    public void gallery(View view)
    {
        // Create new fragment and transaction
        Fragment newFragment = new ChooserFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }








}
