package com.github.newiarch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;


public class TakePictureFragment extends Fragment
        implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;
    static String fileLocation = null;
    static File newFileLocation;
    static private String date;
    static private String projectName;
    static private String projectTitle; // same as projectName but it won't be modified
    static private String location;
    static private String artifact;
    static private String description;
    static double latitude;
    static double longitude;
    static LocationManager locationManager;
    static LocationListener locationListener;
    static Location currentBestLocation;
    static Location lastKnownLocation;
    View view;
    Button dropboxButton;
    int RESULT_OK = -1;
    int RESULT_CANCELED = 0;
    Boolean fileSynced = false;
    Spinner pSpinner;
    static List<String> list;
    private static final int TWO_MINUTES = 1000 * 60 * 2;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_take_picture, container, false);
        //getActionBar().setTitle(R.string.title_fragment_take_picture);

        //Set up spinners
        Spinner afct = (Spinner) view.findViewById(R.id.artifact_name);
        afct.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.artifacts, R.layout.spinner_layout);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        afct.setAdapter(adapter);

        getProjectsForSpinner();
        pSpinner = (Spinner) view.findViewById(R.id.project_name);
        ArrayAdapter<String> pAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_layout, list);
        //ArrayAdapter<CharSequence> pAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.artifacts, R.layout.spinner_layout);
        pAdapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        pSpinner.setPrompt("Select your Project");
        pSpinner.setAdapter(
                new NothingSelectedSpinnerAdapter(
                        pAdapter,
                        R.layout.project_spinner_row_nothing_selected,
                        //R.layout.project_spinner_row_nothing_selected, //Optional
                        getActivity()));

        dropboxButton = (Button) view.findViewById(R.id.sync);
        dropboxButton.setOnClickListener(this);

        if (savedInstanceState == null) {
            //create Intent to take a picture
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            getLocation();
            getDate();

            //Ensure there is a camera activity to handle intent
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                //create file where photo should go
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

                //continue only if file was successfully created
                if (fileUri != null) {
                    //store file path to variable
                    fileLocation = fileUri.getPath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                    //start the image capture Intent
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }
            }
        } else {
            //savedInstanceState is not null
            if (savedInstanceState.containsKey("camera_image")) {
                fileLocation = savedInstanceState.getString("camera_image");
            }
        }

        return view;
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("you just resumed it");

        //user rotated the screen, redraw stuff
        //show picture that was taken
        setPic(fileLocation);

        TextView textDate = (TextView) view.findViewById(R.id.date);
        textDate.setText(date);

        TextView latText = (TextView) view.findViewById(R.id.latitude);
        latText.setText("Latitude: " + latitude);

        TextView longText = (TextView) view.findViewById(R.id.longitude);
        longText.setText("Longitude: " + longitude);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        //delete photo if back button was pressed on TakePicture after taking photo
        if (isRemoving() && !fileSynced) {
            if (fileLocation != null) {
                Toast.makeText(getActivity(), "Back button pressed, deleting image", Toast.LENGTH_SHORT).show();
                File myFile = new File(fileLocation);
                myFile.delete();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //	return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //stop getting location updates; saves battery
        stopLocation();

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            //show picture that was taken
            setPic(fileLocation);

            TextView textDate = (TextView) view.findViewById(R.id.date);
            textDate.setText(date);

            TextView latText = (TextView) view.findViewById(R.id.latitude);
            latText.setText("Latitude: " + latitude);

            TextView longText = (TextView) view.findViewById(R.id.longitude);
            longText.setText("Longitude: " + longitude);

        } else if (resultCode == RESULT_CANCELED) {
            //user cancelled the image capture
            fileSynced = false;
            getActivity().getFragmentManager().popBackStack();
        } else {
            // image capture failed, advise user
            Toast.makeText(getActivity(), "Error Capturing Image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        if (fileLocation != null)
            savedInstanceState.putString("camera_image", fileLocation);

        super.onSaveInstanceState(savedInstanceState);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));

    }

    private static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "iArch");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("iArch", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void capturePictureData() {
        //prevent crash when nothing is selected on spinner
        if (pSpinner.getSelectedItem() != null) {
            projectName = pSpinner.getSelectedItem().toString();
            projectTitle = projectName; // So we can add unedited name to datastore
            //convert projectName to something dropbox will accept as a datastore name
            projectName = projectName.toLowerCase(Locale.US);
            projectName = projectName.replace(" ", "_");
        }
        System.out.println("PROJECT NAME: " + projectName);

        EditText locationEditText = (EditText) view.findViewById(R.id.location_name);
        location = locationEditText.getText().toString();

        //EditText artifactEditText = (EditText) view.findViewById(R.id.artifact_name);
        //artifact = artifactEditText.getText().toString();

        Spinner afct = (Spinner) view.findViewById(R.id.artifact_name);
        artifact = afct.getSelectedItem().toString();

        EditText descriptionEditText = (EditText) view.findViewById(R.id.description);
        description = descriptionEditText.getText().toString();

    }

    //sync to dropbox click
    public void syncToDropbox() {
        String[] splitLoc = fileLocation.split("/");
        capturePictureData();

        if (pSpinner.getSelectedItem() == null) {
            //projectName was null, give error since it is a required field
            Toast.makeText(getActivity(), "Error: Project Name is required", Toast.LENGTH_SHORT).show();

        } else {
            //file to copy
            File myFile = new File(fileLocation);
            //create new project directory under iArch folder
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "iArch/" + projectName);
            //new file to move to
            newFileLocation = new File(mediaStorageDir.toString() + "/" + splitLoc[6]);
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("iArch/" + projectName, " failed to create directory");
                }
            }
            //move file to project folder
            myFile.renameTo(newFileLocation);

            //sync picture with dropbox upon clicking sync button
            if (MainActivity.mDBApi.getSession().isLinked()) {
                Boolean syncCorrectly = dropboxStuff(fileLocation);
                if (syncCorrectly) {
                    fileSynced = true;
                    getActivity().getFragmentManager().popBackStack();
                }
                // Need to add failure message
            }

        }
    }

    private Boolean dropboxStuff(String file) {
        // Get the data entered into the textboxes
        //capturePictureData();
        //shorten path
        String[] splitFile = file.split("/");
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "iArch/" + projectName);
        File[] files = new File[1];
        files[0] = new File(mediaStorageDir + "/" + splitFile[6]);

        Upload upload = new Upload(getActivity(), MainActivity.mDBApi, projectName + "/", files);
        upload.execute();

        if (upload.getStatus() == Upload.Status.PENDING) {
            // My AsyncTask has not started yet
            Log.i("Status pend",
                    " " + upload.getStatus());
        }

        if (upload.getStatus() == Upload.Status.RUNNING) {
            // My AsyncTask is currently doing work in
            // doInBackground()
            Log.i("Status run ",
                    " " + upload.getStatus());
        }

        if (upload.getStatus() == Upload.Status.FINISHED) {
            Log.i("Status Finished",
                    " " + upload.getStatus());
            // My AsyncTask is done and onPostExecute
            // was called
        }

        /*
        File myFile = new File(splitFile[6]);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                DropboxAPI.Entry response = MainActivity.mDBApi.putFile(splitFile[6], inputStream, file.length(), null, null);
                Log.i("DBExampleLog", "The uploaded file's rev is: " + response.rev);
            } catch (DropboxException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
*/
/*
            //get link from dropbox and create remote path for sync; create datastore
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(MainActivity.mAccountManager.getLinkedAccount());
            DbxFile testFile;
            if (projectName != "") {
                testFile = dbxFs.create(new DbxPath(projectName + "/" + splitFile[6]));
                fileLocation = newFileLocation.toString();

                try {
                    //create remote file and assign it to photo
                    File fileVar = new File(fileLocation);
                    testFile.writeFromExistingFile(fileVar, false);
                    //set up dropbox datastores
                    //DbxDatastore datastore = MainActivity.mDatastoreManager.openDefaultDatastore();
                    DbxDatastore datastore = MainActivity.mDatastoreManager.openOrCreateDatastore(projectName);

                    if (datastore.getTitle() == null) // check if a title already exists (aka datastore already exists)
                    {
                        datastore.setTitle(projectTitle); // set the datastore title
                    }

                    DbxTable dataTbl = datastore.getTable("Picture_Data");

                    @SuppressWarnings("unused")
                    DbxRecord task = dataTbl.insert().set("LOCAL_FILENAME", fileLocation).
                            set("DATE", date).
                            set("LATITUDE", latitude).set("LONGITUDE", longitude).
                            set("PROJECT_NAME", projectTitle).
                            set("LOCATION", location).
                            set("ARTIFACT_TYPE", artifact).
                            set("DESCRIPTION", description);

                    //sync datastore
                    datastore.sync();

                    //close datastore
                    datastore.close();

                    return true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    //close remote file so other things can be done
                    testFile.close();
                    //return true;
                }
            } else {
                //user did not enter a valid project name
                Toast.makeText(getActivity(), "Enter a valid Project Name", Toast.LENGTH_SHORT).show();
            }
        } catch (Unauthorized e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (InvalidPathException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DbxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

*/
        return true;

    }

    private void setPic(String file) {
        //get dimensions of view
        ImageView myImage = (ImageView) view.findViewById(R.id.imageView1);

        //this works for now... hard coded scale factor
        int targetW = 900;//myImage.getWidth();
        int targetH = 600;//myImage.getHeight();

        System.out.println("targetW: " + targetW + " targetH: " + targetH);

        Bitmap myBitmap = decodeSampledBitmapFromFile(file, targetW, targetH);
        myImage.setImageBitmap(myBitmap);
    }

    public static Bitmap decodeSampledBitmapFromFile(String file, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }

    @SuppressLint("SimpleDateFormat")
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        System.out.println("INSAMPLE SIZE: " + inSampleSize);
        return inSampleSize;
    }

    void getDate() {
        date = new SimpleDateFormat("EEE, MMM dd, yyyy HH:mm:ss z").format(new Date());
    }

    void getLocation() {
        //acquire a reference to system location manager
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;

        //check to see if last known location exists
        if (locationManager != null) {
            //set cached last known location to current location for initial state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    return;
                }
            }
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation != null) {
                latitude = lastKnownLocation.getLatitude();
                longitude = lastKnownLocation.getLongitude();
            }
        }

        //define listener that responds to location updates
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                // called when new location is found by network location provider
                if (isBetterLocation(location, currentBestLocation)) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    currentBestLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub

            }

        };
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
    }

    void stopLocation() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (locationListener != null) {
                //stop looking for location updates; saves battery
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
                        return;
                    }
                }
                locationManager.removeUpdates(locationListener);
            }
        }
    }

    @Override
    public void onClick(View v) {
        syncToDropbox();
    }

    static void getProjectsForSpinner() {
        list = new ArrayList< >();
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "iArch");
        File[] projectList = mediaStorageDir.listFiles();
        if (projectList != null) {
            for (int i = 0; i < projectList.length; i++) {
                System.out.println("LIST: " + projectList[i].toString());
                if (projectList[i].isDirectory()) {
                    String[] splitList = projectList[i].toString().split("/");
                    list.add(splitList[6]);
                }
            }
        }
        if (list.isEmpty()) {
            list.add("");
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}