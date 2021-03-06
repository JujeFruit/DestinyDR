package net.dungeonrealms.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.dungeonrealms.common.Constants;
import net.dungeonrealms.common.game.database.player.PlayerToken;
import net.dungeonrealms.common.network.ServerAddress;
import net.dungeonrealms.common.network.ShardInfo;
import net.dungeonrealms.network.packet.Packet;
import net.dungeonrealms.network.packet.type.BasicMessagePacket;
import net.dungeonrealms.network.packet.type.ServerListPacket;

import java.io.IOException;
import java.util.UUID;

public class GameClient
        extends Listener {

    private Client client;
    private Runnable reconnected;
    private boolean isConnected = false;

    public GameClient() {
        Constants.build();
        this.client = new Client(Constants.NET_WRITE_BUFFER_SIZE, Constants.NET_READ_BUFFER_SIZE);
        this.client.addListener(this);
        this.client.setKeepAliveTCP(1000);
        this.client.start();

        registerClasses(client.getKryo());
    }

    public void setReconnector(Runnable reconnector) {
        this.reconnected = reconnector;
    }

    public void registerListener(Listener listener) {
        client.addListener(listener);
    }

    public void removeListener(Listener listener) {
        client.removeListener(listener);
    }

    public Client getClient() {
        return this.client;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() throws IOException {
        Log.info("Connecting to " + Constants.MASTER_SERVER_IP + ":" + Constants.MASTER_SERVER_PORT);
        this.client.connect(500000, Constants.MASTER_SERVER_IP, Constants.MASTER_SERVER_PORT);
        isConnected = true;

        Log.info("Master server connection established!");
    }

    public void kill() {
        if (this.client != null) {
            this.client.stop();
        }
    }

    public void sendTCP(byte[] data) {
        BasicMessagePacket packet = new BasicMessagePacket();

        packet.data = data;
        sendTCP(packet);
    }

    public void sendUDP(byte[] data) {
        BasicMessagePacket packet = new BasicMessagePacket();

        packet.data = data;
        sendUDP(packet);
    }

    public void sendNetworkMessage(String task, String message, String... contents) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(task);
        out.writeUTF(message);

        for (String s : contents)
            out.writeUTF(s);

        sendTCP(out.toByteArray());
    }

    private static void registerClasses(Kryo kryo) {
        kryo.register(Packet.class, 0);
        kryo.register(byte.class, 1);
        kryo.register(byte[].class, 2);
        kryo.register(BasicMessagePacket.class, 3);
        kryo.register(ServerListPacket.class, 4);
        kryo.register(ShardInfo.class, 5);
        kryo.register(ServerAddress.class, 6);
        kryo.register(PlayerToken.class, 7);
        kryo.register(PlayerToken[].class, 8);
        kryo.register(UUID.class, 9);
        kryo.register(String.class, 10);
        kryo.register(int.class, 11);
    }


    public void sendTCP(Packet packet) {
        this.client.sendTCP(packet);
    }

    public void sendUDP(Packet packet) {
        this.client.sendUDP(packet);
    }

    public void disconnected(Connection c) {
        Log.warn("Connection lost between master server. Attempting to reestablish connection...");
        Runnable run = new DefaultReconnector();
        if (reconnected != null) run = reconnected;
        new Thread(run).start();
    }

    public class DefaultReconnector
            implements Runnable {
        public DefaultReconnector() {
        }

        public void run() {
            while (!GameClient.this.getClient().isConnected()) {
                try {
                    GameClient.this.client.reconnect();
                } catch (Exception ex) {
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException ex1) {
                        ex.printStackTrace();
                    }
                }
            }

            Log.info("Connection reestablished!");
        }
    }


}
