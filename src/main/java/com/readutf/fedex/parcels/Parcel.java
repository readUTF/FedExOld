package com.readutf.fedex.parcels;

import com.google.gson.JsonObject;

public abstract class Parcel {

    public Parcel() {
    }

    public abstract String getName();
    public abstract JsonObject toJson();
    public abstract void execute(JsonObject jsonObject);

}
