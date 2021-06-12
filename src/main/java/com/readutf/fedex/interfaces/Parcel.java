package com.readutf.fedex.interfaces;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;

public abstract class Parcel {

    public Parcel() {
        FedEx.debug("test");
    }

    public abstract String getName();

    public abstract JsonObject toJson();

}
