package com.github.newiarch;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.io.File;

public class UploadDialog extends DialogFragment {

    public static final String TAG = UploadDialog.class.getSimpleName();
    String projectName;
    String[] files;
    private Upload mTask;
    private ProgressBar mProgressBar;

    public interface UploadDialogCallback {
        void onPreExecute(int maxProgress);

        void onProgressUpdate(int progress);

        void onCancelled();

        void onPostExecute();
    }

    public static UploadDialog newInstance() {
        UploadDialog taskFragment = new UploadDialog();

        return taskFragment;
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            projectName = bundle.getString("EXTRAS_PROJECTNAME");
            files = bundle.getStringArray("EXTRAS_SPLITFILE");
        }
        View view = inflater.inflate(R.layout.dialog_progress_task, container);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(100);

        getDialog().setTitle(getActivity().getString(R.string.accept));
        // This dialog can't be canceled by pressing the back key.
        getDialog().setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);

        return view;
    }

    /**
     * This method will only be called once when the retained Fragment is first
     * created.
     */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(SherlockDialogFragment.STYLE_NORMAL, R.style.TuriosDialog);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "iArch/" + projectName);
        File[] files = new File[1];
        files[0] = new File(mediaStorageDir + "/" + files[0]);
        mTask = new Upload(getActivity(), MainActivity.mDBApi, projectName + "/", files);
        mTask.setCallback(new UploadDialogCallback() {

            @Override public void onPreExecute(int maxProgress) {
            }

            @Override public void onProgressUpdate(int progress) {
                mProgressBar.setProgress(progress);
            }

            @Override public void onPostExecute() {
                if (isResumed())
                    dismiss();

                mTask = null;

            }

            @Override public void onCancelled() {
                if (isResumed())
                    dismiss();

                mTask = null;
            }
        });

        mTask.execute();
        if (mTask.getStatus() == Upload.Status.PENDING) {
            // My AsyncTask has not started yet
            Log.i("Status pend",
                    " " + mTask.getStatus());
        }

        if (mTask.getStatus() == Upload.Status.RUNNING) {
            // My AsyncTask is currently doing work in
            // doInBackground()
            Log.i("Status run ",
                    " " + mTask.getStatus());
        }

        if (mTask.getStatus() == Upload.Status.FINISHED) {
            Log.i("Status Finished",
                    " " + mTask.getStatus());
            // My AsyncTask is done and onPostExecute
            // was called
        }
    }

    @Override public void onResume() {
        super.onResume();

        // This is a little hacky, but we will see if the task has finished
        // while we weren't
        // in this activity, and then we can dismiss ourselves.
        if (mTask == null)
            dismiss();
    }

    @Override public void onDetach() {
        super.onDetach();
    }

    // This is to work around what is apparently a bug. If you don't have it
    // here the dialog will be dismissed on rotation, so tell it not to dismiss.
    @Override public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    // Also when we are dismissed we need to cancel the task.
    @Override public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        // If true, the thread is interrupted immediately, which may do bad
        // things.
        // If false, it guarantees a result is never returned (onPostExecute()
        // isn't called)
        // but you have to repeatedly call isCancelled() in your
        // doInBackground()
        // function to check if it should exit. For some tasks that might not be
        // feasible.
        if (mTask != null)
            mTask.cancel(false);

    }
}
