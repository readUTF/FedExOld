package com.readutf.fedex;

import com.google.gson.JsonParser;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

public class FedExSubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {

        try {
            FedEx.LOGGER.log(Level.SEVERE, message);
            String[] parts = message.split(";");
            String name = parts[0];
            String jsonRaw = parts[1];
            String id = parts[2];
            FedEx.LOGGER.log(Level.SEVERE, "Parcel received: [" + id + "]  [" + name + "]  [" + jsonRaw + "]");
            FedEx.getInstance().getParcels().stream().filter(parcel -> parcel.getName().equalsIgnoreCase(name) && !FedEx.getInstance().getId().toString().equalsIgnoreCase(id)).forEach(parcel -> {
                parcel.execute(new JsonParser().parse(jsonRaw).getAsJsonObject());
            });
        } catch (Exception ignore) {}


    }
}
