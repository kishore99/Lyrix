package com.example.kishore_yuva.lyrix.Activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.kishore_yuva.lyrix.Constants;
import com.example.kishore_yuva.lyrix.R;
import com.example.kishore_yuva.lyrix.Adapters.SongsAdapter;
import com.example.kishore_yuva.lyrix.Data.SongContract;
import com.example.kishore_yuva.lyrix.Services.ApiIntentService;
import com.example.kishore_yuva.lyrix.Services.DataIntentService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import static com.example.kishore_yuva.lyrix.Utilities.md5;

public class SearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private RecyclerView recycler;
    private SongsAdapter adapter;
    private EditText searchText;
    private Button searchButton;
    int lastFirstVisiblePosition=0;
    GridLayoutManager gridLayoutManager;

    private AdView mAdView;
    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Initialize by clearing search table
        Intent clear = new Intent(getActivity(), DataIntentService.class);
        clear.setAction(DataIntentService.ACTION_DELETE);
        getActivity().startService(clear);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        lastFirstVisiblePosition = gridLayoutManager.findFirstCompletelyVisibleItemPosition();
        outState.putInt("position",lastFirstVisiblePosition);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
//        lastFirstVisiblePosition=savedInstanceState.getInt("position");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        recycler = (RecyclerView) v.findViewById(R.id.recycler);
        searchButton = (Button) v.findViewById(R.id.button_search);
        searchText = (EditText) v.findViewById(R.id.searchText);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(),2));
        if(savedInstanceState!=null){
            lastFirstVisiblePosition= savedInstanceState.getInt("position");
        }
        adapter = new SongsAdapter(getActivity(), false);
        recycler.setAdapter(adapter);
        recycler.scrollToPosition(lastFirstVisiblePosition);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear before each search
                Intent clear = new Intent(getActivity(), DataIntentService.class);
                clear.setAction(DataIntentService.ACTION_DELETE);
                getActivity().startService(clear);

                // Perform search
                String text = searchText.getText().toString();
                String url = Constants.getLFSearchURL(text);
                Intent i = new Intent(getActivity(), ApiIntentService.class);
                i.setAction(ApiIntentService.ACTION_SEARCH);
                i.putExtra("url", url);
                getActivity().startService(i);

                hideKeyboard();
            }
        });

        MobileAds.initialize(getActivity(), getString(R.string.adAppId));

        mAdView = (AdView) v.findViewById(R.id.adView);


        String android_id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase();

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(deviceId)
                .build();
        mAdView.loadAd(adRequest);

        return v;
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdView != null) {
            mAdView.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Intent clear = new Intent(getActivity(), DataIntentService.class);
        clear.setAction(DataIntentService.ACTION_DELETE);
        getActivity().startService(clear);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = SongContract.SearchEntry.CONTENT_URI;
        return new CursorLoader(getActivity(),
                uri,
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null) {
            adapter.setCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setCursor(null);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void hideKeyboard() {
        InputMethodManager inputManager =
                (InputMethodManager) getActivity().
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }
}