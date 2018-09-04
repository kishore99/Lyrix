package com.example.kishore_yuva.lyrix.Activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kishore_yuva.lyrix.Constants;
import com.example.kishore_yuva.lyrix.R;
import com.example.kishore_yuva.lyrix.Adapters.SongsAdapter;
import com.example.kishore_yuva.lyrix.Data.SongContract;
import com.example.kishore_yuva.lyrix.Services.ApiIntentService;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class FeedFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

   /* private String mParam1;
    private String mParam2;*/

    private OnFragmentInteractionListener mListener;
    private SongsAdapter adapter;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest locationRequest;

    private CountryReceiver receiver;

    private final int REQUEST_LOCATION = 1;


    private AdView adView;
    //0 - Popular, 1 - Recent, 2 - Favourites
    private int type = 0;
    public static final int COL_TITLE = 0;
    public static final int COL_ARTIST = 1;
    public static final int COL_IMAGE_URL = 2;
    public static final int COL_LYRICS = 3;
    public static final int COL_RECENT = 4;


    public FeedFragment() {
        // Required empty public constructor
    }

    public static FeedFragment newInstance(String param1, String param2) {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only initialize for Popular Feed fragment (gets popular songs based on location)
        if (type == 0 && mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(Objects.requireNonNull(getActivity()))
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_feed, container, false);
        RecyclerView recycler = (RecyclerView) v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        if (type == 1 || type == 2)
            adapter = new SongsAdapter(getActivity(), true);
        else
            adapter = new SongsAdapter(getActivity(), false);

        recycler.setAdapter(adapter);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onStart() {
        if (type == 0)
            mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        if (type == 0)
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register Receiver if we are getting Top movies of region
        if (type == 0) {
            IntentFilter filter = new IntentFilter(CountryReceiver.PROCESS_COUNTRY_RESPONSE);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            receiver = new CountryReceiver();
            getActivity().registerReceiver(receiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (type == 0) {
            getActivity().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;

        if (type == 0) {
            uri = SongContract.SearchEntry.CONTENT_URI;
        } else {
            int isRecent = (type == 1) ? 1 : 0;
            uri = SongContract.SongEntry.buildSongRecentUri(isRecent);
        }

        return new CursorLoader(getActivity(),
                uri,
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            adapter.setCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setCursor(null);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            verifyLocationSettings();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("Google API Client", "Google API client not able to connect");
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verifyLocationSettings();
            } else {
                Log.d("Location Permission", "Location Permission was denied");
            }
        }
    }

    public void requestLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            mLastLocation = location;
            Log.d("Last location", "Lat : " + mLastLocation.getLatitude());
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            startGeoNameRequest();
        }
    }


    public void verifyLocationSettings() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        requestLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(getActivity(), 1000);
                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                }
            }
        });
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000 && resultCode == RESULT_OK) {
            requestLocationUpdates();
        }
    }

    public void setType(int type) {
        this.type = type;
    }

    public void startGeoNameRequest() {
        Intent i = new Intent(getActivity(), ApiIntentService.class);
        i.setAction(ApiIntentService.ACTION_GEONAME);
        float lat = (float)mLastLocation.getLatitude();
        float lon = (float)mLastLocation.getLongitude();
        i.putExtra(ApiIntentService.EXT_LON, lon);
        i.putExtra(ApiIntentService.EXT_LAT, lat);
        getActivity().startService(i);
    }

    public class CountryReceiver extends BroadcastReceiver {

        public static final String PROCESS_COUNTRY_RESPONSE = "com.example.kishore_yuva.lyrix.intent.action.PROCESS_COUNTRY_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            String country = intent.getStringExtra(ApiIntentService.RESPONSE_COUNTRY);
            Log.d("Geoname", "Received geoname " + country);
            // Start intent service to obtain pop songs
            Intent i = new Intent(getActivity(), ApiIntentService.class);
            i.setAction(ApiIntentService.ACTION_TOP_TRACKS);
            i.putExtra(ApiIntentService.EXT_URL, Constants.getLFTopTracksURL(country));
            getActivity().startService(i);
        }
    }
}
