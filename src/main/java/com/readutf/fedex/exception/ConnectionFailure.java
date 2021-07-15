package com.readutf.fedex.exception;

public class ConnectionFailure extends Exception{

    public ConnectionFailure(String reason) {
        super("Connection failed: " + reason);
    }
}
