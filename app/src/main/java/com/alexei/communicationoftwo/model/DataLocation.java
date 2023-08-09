package com.alexei.communicationoftwo.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;


public class DataLocation implements Parcelable {
    double latitude = 0;
    double longitude = 0;


    public DataLocation() {
    }

    public DataLocation(Location location) {

        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
    }

    public DataLocation(LatLng latLng) {

        this.latitude = (double) Math.round(latLng.latitude * 100000d) / 100000d;
        this.longitude = (double) Math.round(latLng.longitude * 100000d) / 100000d;
    }

    public DataLocation(DataLocation location) {

        this.latitude = location.latitude;
        this.longitude = location.longitude;
    }

    public DataLocation(double latitude, double longitude) {

        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected DataLocation(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<DataLocation> CREATOR = new Creator<DataLocation>() {
        @Override
        public DataLocation createFromParcel(Parcel in) {
            return new DataLocation(in);
        }

        @Override
        public DataLocation[] newArray(int size) {
            return new DataLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
