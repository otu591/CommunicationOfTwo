
package com.alexei.communicationoftwo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.databinding.ActivitySelectPointInMapBinding;
import com.alexei.communicationoftwo.exClass.BuildLocationClass;
import com.alexei.communicationoftwo.model.DataLocation;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class SelectPointInMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private Polyline line;
    private GoogleMap mMap;
    private Marker dotMyMarker;
    private Marker dotFromMarker;
    private float mZoom = 16;
    private ActivitySelectPointInMapBinding binding;
    private BuildLocationClass locationClass;
    private Location location = new Location("");
    private boolean defineDotFrom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectPointInMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapSelDot);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getParcelableExtra("location") != null) {
                DataLocation loc = intent.getParcelableExtra("location");
                location.setLatitude(loc.getLatitude());
                location.setLongitude(loc.getLongitude());
            } else {
                if (checkPermission()) {
                    runBuildLocationClass();
                }
            }
        }

        viewListeners();
    }


    private void runBuildLocationClass() {
        if (locationClass == null) {
            locationClass = new BuildLocationClass();//.getInstance();
            handlerLocationClassListeners();

        }
    }

    private void viewListeners() {
        binding.ibSpeakAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceActivity();
            }
        });

        binding.ibFindAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findAddress(binding.etAddress.getText().toString());

            }
        });

        binding.ibMyPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dotMyMarker != null) {
                    displayAllMarkersByCenter();
                }
            }
        });

        binding.butOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (locationClass != null)
                    locationClass.recoveryResources();

                selectedDot();
            }
        });

        binding.etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    binding.ibSpeakAddress.setVisibility(View.GONE);

                } else {
                    binding.ibSpeakAddress.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void startVoiceActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        Locale current = getResources().getConfiguration().locale;
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, current);//Locale устанавливается в настроиках!!!!!!!!!!!!!!!!!!!!!

        startActivityForResult(intent, Const.VOICE_REQUEST_CODE);
    }

    private void handlerLocationClassListeners() {

        locationClass.setOnListeners(new BuildLocationClass.OnLocationListeners() {

            @Override
            public void onUpdateLocation(Location loc, int satellites) {

                location.set(loc);

                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                if (mMap != null) {
                    if (dotMyMarker != null) {
                        dotMyMarker.setPosition(pos);
                    } else {
                        dotMyMarker = mMap.addMarker(new MarkerOptions().position(pos).title(getString(R.string.I)));
                    }
                    //если касание экрана не было
                    if (!defineDotFrom) {
                        if (dotFromMarker != null) {
//                            устанавливаем позицию
                            dotFromMarker.setPosition(pos);
                        } else {
//                            создаем
                            dotFromMarker = mMap.addMarker(new MarkerOptions().position(pos).title(getString(R.string.select_dot)));
                        }
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
                    }
                }

//                updateUi(satellites);
            }

            @Override
            public void onRunning() {

                binding.ibMyPos.setColorFilter(Color.BLUE);
            }

            @Override
            public void onStop() {
                binding.ibMyPos.setColorFilter(Color.RED);
                location = new Location("");
                locationClass = null;
            }

            @Override
            public void onGetPermission(Exception e) {
                try {

                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    resolvableApiException.startResolutionForResult(SelectPointInMapActivity.this, Const.CHECK_SETTINGS_CODE);//----показать окно этой настройки
                } catch (IntentSender.SendIntentException sie) {
                    // Log the error
                    sie.printStackTrace();
                }
            }

            @Override
            public void onChangeUnavailable() {
                Toast.makeText(SelectPointInMapActivity.this, R.string.debug_change_setting_to_device, Toast.LENGTH_LONG).show();
            }
        });
        locationClass.startLocationUpdate();
    }


    @Override
    public void onBackPressed() {
        if (locationClass != null) {

            locationClass.recoveryResources();
        }
        super.onBackPressed();
    }


    private void drawLineRoute() {
        if (line != null) line.remove();

        line = mMap.addPolyline(new PolylineOptions().add(dotMyMarker.getPosition(), dotFromMarker.getPosition())
                .color(Color.RED)
                .width(2));

        binding.tvDistanceToDot.setText(defineDistance(dotMyMarker.getPosition().latitude, dotMyMarker.getPosition().longitude,
                dotFromMarker.getPosition().latitude, dotFromMarker.getPosition().longitude));

    }

    private String defineDistance(double latFrom, double longFrom, double latTo, double longTo) {
//            float distance = locationFrom.distanceTo(locationTo);//-----------------расчет расстояния от водителя до пользователя
        float distance = calculationDistance(latFrom, longFrom, latTo, longTo);//-----------------расчет расстояния от водителя до пользователя
        return (distance < 1000) ? ("" + (int) distance + getString(R.string.m)) : (String.format("%.3f", distance / 1000) + getString(R.string.km));
    }

    private float calculationDistance(double fromLat, double fromLong, double toLat, double toLong) {
        float[] results = new float[1];
        Location.distanceBetween(fromLat, fromLong, toLat, toLong, results);

        return results[0];
    }

    private void displayAllMarkersByCenter() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (dotMyMarker != null) {
            builder.include(dotMyMarker.getPosition());
        }
        if (dotFromMarker != null) {
            builder.include(dotFromMarker.getPosition());
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        mMap.setMaxZoomPreference(18);
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mZoom = 14;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        mZoom = mMap.getCameraPosition().zoom;
                    }
                });

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {

                        dotFromMarker.setPosition(latLng);

                        defineDotFrom = true;

                        drawLineRoute();

                    }
                });

                initMarkers();

                displayAllMarkersByCenter();
            }
        });

    }

    private void initMarkers() {
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        if (dotMyMarker == null) {
            dotMyMarker = mMap.addMarker(new MarkerOptions().position(pos).title(getString(R.string.I)));
        }
        if (dotFromMarker == null) {
            dotFromMarker = mMap.addMarker(new MarkerOptions().position(pos).title(getString(R.string.select_dot)));
        }
    }

    private void setPosFromMarker(LatLng latLng) {

        if (dotFromMarker != null) {
            defineDotFrom = true;

            dotFromMarker.setPosition(latLng);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(mZoom)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            drawLineRoute();
        }


    }


    public void selectedDot() {
        Intent intent = new Intent();
        intent.putExtra("location", new DataLocation(dotFromMarker.getPosition().latitude, dotFromMarker.getPosition().longitude));

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        setResult(Const.RESULT_OK, intent);

        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Const.CHECK_SETTINGS_CODE:
                if (resultCode == Const.RESOLVABLE_RESULT_NO) {

                } else if (resultCode == Const.RESOLVABLE_RESULT_OK) {
                    runBuildLocationClass();
                }
                break;
            case Const.VOICE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    voiceMessageInText(data);
                }
                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Const.LOCATION_REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runBuildLocationClass();
                }
                break;
            }
        }
    }

    private void voiceMessageInText(Intent data) {
        ArrayList<String> texts = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        findAddress(texts.get(0));

    }

    private void findAddress(String locationString) {

        try {
            List<Address> addressList = getAddress(locationString);

            if (addressList.size() > 0) {
                Address address = addressList.get(0);
                setPosFromMarker(new LatLng(address.getLatitude(), address.getLongitude()));
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_found, Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("SelectPointInMapActivity ERROR findAddress(String locationString) - " + e.getMessage());
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<Address> getAddress(String locationString) throws ExecutionException, InterruptedException {

        Callable task = () -> {
            Geocoder gc = new Geocoder(this, Locale.getDefault());
            return gc.getFromLocationName(locationString, 1);
        };

        FutureTask<List<Address>> future = new FutureTask<>(task);
        new Thread(future).start();

        return future.get();
    }

    private boolean checkPermission() {
        System.out.println("73_73");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, Const.LOCATION_REQUEST_PERMISSION);
            return false;
        }
    }

}