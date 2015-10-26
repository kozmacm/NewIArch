package com.github.newiarch;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Upload extends AsyncTask<Void, Long, Boolean> {

    private DropboxAPI<?> mApi;
    private String mPath;

    private DropboxAPI.UploadRequest mRequest;
    private Context mContext;
    //private ProgressDialog mDialog;

    static String mErrorMsg,filename;
    UploadDialog.UploadTaskCallback mUploadTaskCallback;

    // new class variables:
    private int mFilesUploaded;
    static File[] mFilesToUpload;
    private int mCurrentFileIndex;

    int totalBytes = 0, indBytes = 0;

    public Upload(Context context, DropboxAPI<?> api, String dropboxPath,
                  File[] filesToUpload) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mApi = api;
        mPath = dropboxPath;

        // set number of files uploaded to zero.
        mFilesUploaded = 0;
        mFilesToUpload = filesToUpload;
        mCurrentFileIndex = 0;

        for (int i = 0; i < mFilesToUpload.length; i++) {
            Long bytes = mFilesToUpload[i].length();
            totalBytes += bytes;
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            for (int i = 0; i < mFilesToUpload.length; i++) {
                mCurrentFileIndex = i;
                File file = mFilesToUpload[i];

                int bytes = (int) mFilesToUpload[i].length();
                indBytes = bytes;

                filename= mFilesToUpload[i].getName();


                // By creating a request, we get a handle to the putFile
                // operation,
                // so we can cancel it later if we want to
                FileInputStream fis = new FileInputStream(file);
                String path = mPath + file.getName();
                mRequest = mApi.putFileOverwriteRequest(path, fis,
                        file.length(), new ProgressListener() {
                            @Override
                            public long progressInterval() {
                                // Update the progress bar every half-second or
                                // so
                                return 100;
                            }

                            @Override
                            public void onProgress(long bytes, long total) {
                                if (isCancelled()) {
                                    // This will cancel the putFile operation
                                    mRequest.abort();
                                } else {
                                    publishProgress(bytes);
                                }
                            }
                        });

                mRequest.upload();

                if (!isCancelled()) {
                    mFilesUploaded++;
                } else {
                    return false;
                }
            }
            return true;
        } catch (DropboxUnlinkedException e) {
            // This session wasn't authenticated properly or user unlinked
            mErrorMsg = "This app wasn't authenticated properly.";
        } catch (DropboxFileSizeException e) {
            // File size too big to upload via the API
            mErrorMsg = "This file is too big to upload";
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Upload canceled";
        } catch (DropboxServerException e) {
            // Server-side exception. These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them. You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        } catch (FileNotFoundException e) {
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int) (100.0 * (double) progress[0] / indBytes + 0.5);
        Log.i("pro", percent + "    " + progress[0] + "/" + indBytes);
        mUploadTaskCallback.onProgressUpdate(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mUploadTaskCallback.onPostExecute();
        if (result) {
            showToast("Upload finished");
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }

    public void setCallback(UploadDialog.UploadTaskCallback callback) {
        this.mUploadTaskCallback = callback;
    }
}
