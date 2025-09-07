package com.android.location.reactive.locationmanager;

import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.location.LocationListenerCompat;

import java.util.List;

public class LocationListenerImpl implements LocationListenerCompat {
    private final LocationServiceBridge bridge;

    public LocationListenerImpl(LocationServiceBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        bridge.onLocationChanged(location);
    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        for (Location location : locations) {
            onLocationChanged(location);
        }
    }

    @Override
    public void onFlushComplete(int requestCode) {
      //  Timber.d("onFlushComplete requestCode:%s", requestCode);
    }

    @Override
    public void onStatusChanged(@NonNull String provider, int status, Bundle extras) {
        // Timber.d("onFlushComplete provider:%s status:%s extras:%s", provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        bridge.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        bridge.onProviderDisabled(provider);
    }
}
