package com.readutf.fedex;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.readutf.fedex.interfaces.IncomingParcelListener;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class FedExThread implements Runnable {

    FedEx fedEx;

    public FedExThread(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    boolean stopped;

    @Override
    public void run() {
        FedEx.debug("started");
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                FedEx.debug(message);
                if (stopped) {
                    return;
                }


                String[] parts = message.split(";");
                if(FedEx.get().getId().toString().equalsIgnoreCase(parts[2])) {
                    return;
                }
                String name = parts[0];


                FedEx.debug("Parcel received: " + name);

                try {
                    for (Class<?> clazz : fedEx.getPacketListeners()) {
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(IncomingParcelListener.class)) {
                                if (JsonObject.class.isAssignableFrom(method.getParameters()[0].getType())) {
                                    IncomingParcelListener incomingParcelListener = method.getAnnotation(IncomingParcelListener.class);
                                    if (!incomingParcelListener.name().equalsIgnoreCase(name)) {
                                        continue;
                                    }
                                    method.invoke(clazz.newInstance(), new JsonParser().parse(parts[1]));

                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        ForkJoinPool.commonPool().execute(() ->  fedEx.getJedisSubscriber().subscribe(jedisPubSub, fedEx.getChannel()));
    }


}
