package just4fun.poketracker;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private ProgressDialog dialog;
    private LocationManager mLocationManager;
    private GoogleMap mMap;
    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        dialog = new ProgressDialog(this);
        this.dialog.setMessage("Retrieving pokemons...");
        this.dialog.show();
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this);
        getPokemons(-6.918076753627167,107.59408950805664);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng devicePos = new LatLng(ResourceVariable.latitudeDefault, ResourceVariable.longitudeDefault);
        mMap.addMarker(new MarkerOptions().position(devicePos).title("Your Position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(devicePos));
    }

    @Override
    public void onLocationChanged(Location location) {
        //retrieve device location
        ResourceVariable.deviceLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void getPokemons(double lat, double lng){
        String url = "https://pokevision.com/map/data/"+lat+"/"+lng;
        System.out.println(url);
        mRequestQueue = Volley.newRequestQueue(this);
        GsonRequest<ResponseContainer> myReq = new GsonRequest<ResponseContainer>(
                Request.Method.GET,
                url,
                ResponseContainer.class,
                new com.android.volley.Response.Listener<ResponseContainer>() {
                    @Override
                    public void onResponse(ResponseContainer response) {
                        System.out.println("target response retrieved");
                        for(Pokemon pokemon : response.pokemon){
                            addPokemon(pokemon);
                        }
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("response error");
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Toast.makeText(MapsActivity.this, "Failed to retrieve", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        myReq.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(myReq);
    }

    private void addPokemon(final Pokemon pokemon) {
        mRequestQueue = Volley.newRequestQueue(this);
        ImageRequest request = new ImageRequest("https://ugc.pokevision.com/images/pokemon/"+pokemon.pokemonId+".png", new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                LatLng markerLocation = new LatLng(pokemon.latitude, pokemon.longitude);
                mMap.addMarker(new MarkerOptions().position(markerLocation)
                        .title("Pokemon id : "+pokemon.pokemonId)
                        .icon(BitmapDescriptorFactory.fromBitmap(response)));
            }
        }, 0, 0, null,
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("failed to retrieve icon");
                    }
                });
        request.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(request);
    }
}
