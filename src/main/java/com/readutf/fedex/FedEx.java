package com.readutf.fedex;

import com.readutf.fedex.interfaces.IncomingParcelListener;
import com.readutf.fedex.interfaces.Parcel;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Setter;
import redis.clients.jedis.Jedis;
import sun.misc.Unsafe;

@Getter
public class FedEx {

    private List<Class<?>> packetListeners = new ArrayList<>();
    private HashMap<String, Parcel> registeredParcels = new HashMap<>();
    private Thread redisThread;

    private Jedis jedisSubscriber;
    private Jedis jedisPublisher;

    private String channel;
    private Logger logger;

    @Getter @Setter private static boolean debugMode = false;

    private UUID id;

    private static FedEx instance;

    public FedEx(String address, int port, boolean auth, String password, String channel) {
        instance = this;
        id = UUID.randomUUID();
        this.logger = Logger.getLogger(getClass().getName());

        jedisSubscriber = new Jedis(address, port);
        jedisPublisher = new Jedis(address, port);

        if(auth) {
            jedisSubscriber.auth(password);
            jedisPublisher.auth(password);
        }

        new Thread(new FedExThread(this)).start();


        this.channel = channel;


    }

    public boolean connect() {
        try {
            jedisSubscriber.connect();
            jedisPublisher.connect();
        } catch (Exception e) {
            return false;
        }

        new FedExThread(this).run();
        return true;
    }

    public void disconnect() {

        redisThread.interrupt();

        redisThread = null;
        jedisPublisher.disconnect();
        jedisPublisher.disconnect();
    }

    public static FedEx get() {return instance;}


    public void sendParcel(Parcel parcel) {
        jedisPublisher.publish(channel, parcel.getName() +";" + parcel.toJson() + ";" + id.toString());
    }

    public void registerParcelListener(Class<?> clazz) {

        if(Arrays.stream(clazz.getMethods()).noneMatch(method -> method.isAnnotationPresent(IncomingParcelListener.class))) {
            return;
        }
        packetListeners.add(clazz);
    }

    public void registerParcel(Class<?> clazz)  {
        if(clazz == null) return;
        if(!clazz.getSuperclass().equals(Parcel.class)) {
            return;
        }


        Parcel parcel = null;
        try {
            parcel = (Parcel) unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(parcel.getName().equalsIgnoreCase("") || parcel.getName() == null) {
            logger.severe("[" + clazz.getName() + "] Invalid name.");
            return;
        }

        FedEx.debug("registered parcel: " + parcel);
        registeredParcels.put(parcel.getName(), parcel);

    }

    public static void debug(String message) {
        if(!debugMode) return;
        System.out.println(message);
    }

    static Unsafe unsafe;
    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
