package com.github.newiarch;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private boolean mLoggedIn;
    static Button mLinkButton;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //getActionBar().setTitle(R.string.title_fragment_main);

        mLinkButton = (Button) rootView.findViewById(R.id.link_button);

        //dropbox button listener
        mLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickLinkToDropbox();
            }
        });

        //make sure dropbox button is displaying correct text
        if (MainActivity.mDBApi.getSession().isLinked()) {
            showLinkedView();
        } else {
            showUnlinkedView();
        }

        //Display the proper UI state if logged in or not
        setLoggedIn(MainActivity.mDBApi.getSession().isLinked());

        return rootView;
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    private void onClickLinkToDropbox() {

        // This logs you out if you're logged in, or vice versa
        if (mLoggedIn) {
            logOut();
        } else {
            // Start the remote authentication
            MainActivity.mDBApi.getSession().startOAuth2Authentication(getActivity());
        }
    }

    private void showLinkedView() {
        mLinkButton.setText("Unlink from Dropbox");
    }

    private void showUnlinkedView() {
        mLinkButton.setText("Connect to Dropbox");
    }

    private void logOut() {
        // Remove credentials from the session
        MainActivity.mDBApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    private void clearKeys() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.clear();
        edit.commit();
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            mLinkButton.setText("Unlink from Dropbox");
            //mDisplay.setVisibility(View.VISIBLE);
        } else {
            mLinkButton.setText("Link with Dropbox");
            //mDisplay.setVisibility(View.GONE);
            //mImage.setImageDrawable(null);
        }
    }
}
