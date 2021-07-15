package com.readutf.fedex;

import com.readutf.fedex.exception.ConnectionFailure;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.utils.ClassUtils;
import com.readutf.fedex.utils.UnsafeHandler;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class FedEx {

    UUID id;

    String address;
    int port;
    boolean auth;
    String password;
    String channel;

    @Getter
    private static FedEx instance;
    public static Logger LOGGER;

    @Getter
    private static boolean redisDisconnected;
    @Getter @Setter private static boolean debug = false;

    private Thread jedisThread;

    private FedExSubscriber fedExSubscriber;


    private Jedis publisher;
    private Jedis subscriber;
    private Jedis dataHandle;


    @Getter
    List<Parcel> parcels;
    Thread subscriberThread;

    public FedEx(String address, int port, boolean auth, String password, String channel) throws ConnectionFailure {
        instance = this;
        this.address = address;
        this.port = port;
        this.auth = auth;
        this.channel = channel;
        this.password = password;
        id = UUID.randomUUID();
        LOGGER = Logger.getLogger(getClass().getName());
        fedExSubscriber = new FedExSubscriber();

        parcels = new ArrayList<>();

        publisher = new Jedis(address, port);
        subscriber = new Jedis(address, port);
        dataHandle = new Jedis(address, port);

        try {
            jedisThread = new Thread(() -> ForkJoinPool.commonPool().execute(() -> subscriber.subscribe(fedExSubscriber, channel)));
            jedisThread.start();
        } catch (JedisConnectionException e) {
            redisDisconnected = true;
            throw new ConnectionFailure("Could not connect to host " + address + ":" + port);
        }
    }

    public void disconnect() {
        if(fedExSubscriber.isSubscribed()) fedExSubscriber.unsubscribe();
        jedisThread.interrupt();
        subscriber.quit();
        publisher.quit();
    }

    public void sendParcel(Parcel parcel) {
        if (redisDisconnected) return;
        publisher.publish(channel, parcel.getName() + ";" + parcel.toJson() + ";" + id.toString());
    }

    /*
      @Param object is the main class
     */
    public void registerParcels(Object object) {
        Class<?> clazz = object.getClass();
        ClassUtils.getClassesInPackage(clazz, clazz.getPackage().getName()).stream().filter(Parcel.class::isAssignableFrom).forEach(this::registerParcel);
    }

    public void registerParcel(Class<?> clazz) {
        if (clazz == null) return;
        if (!clazz.getSuperclass().equals(Parcel.class)) {
            return;
        }
        Parcel parcel = new UnsafeHandler<Parcel>(clazz).getInstance();
        if (parcel == null || parcel.getName() == null || parcel.getName().equalsIgnoreCase("")) {
            debug("[" + clazz.getName() + "] Invalid name.");
            return;
        }
        debug("registered parcel: " + parcel.getName());
        if (parcels.stream().noneMatch(parcel1 -> parcel1.getName().equalsIgnoreCase(parcel.getName()))) {
            parcels.add(parcel);
        }
    }

    public static void debug(String message) {
        if(debug) System.out.println(message);
    }

    public static Jedis getResource() {
        return getInstance().dataHandle;
    }

}
