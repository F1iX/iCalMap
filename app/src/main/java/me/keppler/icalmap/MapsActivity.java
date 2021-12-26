package me.keppler.icalmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getName();

    private List<Event> events = new ArrayList<>();

    private static final int REQUEST_CODE_KEEP_EVENTS = 111;
    private static final int REQUEST_CODE_CLEAR_EVENTS = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Find buttons
        ImageView addFileBtn = (ImageView) mapFragment.getView().findViewById(R.id.add_file_btn);

        addFileBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Log.d(TAG, "Add file button");

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.add_file_dialog_title);
                builder.setMessage(R.string.add_file_dialog_text);
                builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Clear button");
                        filePickerIntent(REQUEST_CODE_CLEAR_EVENTS);
                    }
                });
                builder.setNegativeButton(R.string.keep, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Keep button");
                        filePickerIntent(REQUEST_CODE_KEEP_EVENTS);
                    }
                });
                builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Cancel button");
                    }
                });
                builder.show();
            }
        });
    }

    private void filePickerIntent(int requestCode){
        Intent intent = new Intent()
                .setType("text/calendar")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select a file"), requestCode);
    }

    // File selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQUEST_CODE_KEEP_EVENTS | requestCode == REQUEST_CODE_CLEAR_EVENTS) && resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CLEAR_EVENTS){
                clear_events();
            }
            Uri selectedfile = data.getData(); // URI with the location of the selected file
            Log.d(TAG, "File selected");
            try {
                InputStream is = getContentResolver().openInputStream(selectedfile);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                String organizer = "", title = "", street = "", description = "";
                int parsed = 0;
                while ((line = br.readLine()) != null) {
                    //Log.d("test", line);
                    if (line.startsWith("ORGANIZER")) {
                        organizer = line.substring(line.indexOf("CN=") + 4, line.indexOf(":MAILTO") - 1).replace("\\", "");
                        Log.v("org", organizer);
                        parsed = 1;
                    }
                    if (line.startsWith("SUMMARY") & parsed == 1) {
                        title = line.substring(8).replace("\\", "");
                        Log.v("tit", title);
                        parsed++;
                    }
                    if (line.startsWith("DESCRIPTION") & parsed == 2) {
                        description = line.substring(12).replace("\\", "");
                        Log.v("desc", description);
                        parsed++;
                    }
                    if (line.startsWith("LOCATION") & parsed == 3) {
                        street = line.substring(9).replace("\\", "");
                        Log.v("ad", street);

                        events.add(new Event(organizer, title, street, description));
                        parsed = 0;  // reset to start with next event
                    }
                }
                Log.v("total", String.valueOf(events.size()));
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Populate parsed events on map
        populate_event_markers();
    }

    /**
     * Manipulate the map once available
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        populate_event_markers();

        mMap.moveCamera(CameraUpdateFactory.zoomTo(13)); // city level
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(51.05,13.73))); // Dresden

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context mContext = getApplicationContext();

                LinearLayout info = new LinearLayout(mContext);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(mContext);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    public void populate_event_markers(){
        Geocoder geocoder = new Geocoder(this);

        for (Event event : events) {
            try{
                // Estimate full address from given street and identify coordinates
                event.address = geocoder.getFromLocationName(event.street, 1).get(0);
                LatLng location = new LatLng(event.address.getLatitude(), event.address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(location).title(event.title).snippet(event.description + "\n\n" + event.organizer + "\n" + event.street));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Clear event list
    public void clear_events(){
        events.clear();
        mMap.clear();
    }
}