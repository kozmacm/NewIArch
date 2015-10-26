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
    String files;
    static Upload mTask;
    static ProgressBar mProgressBar;

    public interface UploadTaskCallback {
        void onPreExecute(int maxProgress);

        void onProgressUpdate(int progress);

        void onCancelled();

        void onPostExecute();
    }

    public static UploadDialog newInstance(String projectName, String files) {
        UploadDialog taskFragment = new UploadDialog();
        taskFragment.projectName = projectName;
        taskFragment.files = files;

        return taskFragment;
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_progress_task, container);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(100);

        getDialog().setTitle("Uploading file 1 / " + Upload.mFilesToUpload.length);
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
        String[] splitFile = files.split("/");

        //Testing... to be removed later
        System.out.println("PROJECT NAME: " + projectName);
        System.out.println("splitFile[6]: " + splitFile[6]);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "iArch/" + projectName);
        File[] theFiles = new File[1];
        theFiles[0] = new File(mediaStorageDir + "/" + splitFile[6]);
        mTask = new Upload(getActivity(), MainActivity.mDBApi, projectName + "/", theFiles);
        mTask.setCallback(new UploadTaskCallback() {

            @Override
            public void onPreExecute(int maxProgress) {

            }

            @Override
            public void onProgressUpdate(int progress) {
                mProgressBar.setProgress(progress);
            }

            @Override
            public void onCancelled() {
                if (isResumed())
                    dismiss();

                mTask = null;
            }

            @Override
            public void onPostExecute() {
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
