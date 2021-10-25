package wow.sniffer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.jboss.logging.Logger;
import wow.sniffer.entity.*;
import wow.sniffer.net.Packet;
import wow.sniffer.net.PacketHandler;
import wow.sniffer.net.PacketReader;

import javax.persistence.Persistence;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    private final Logger log = Logger.getLogger(Server.class);

    private final BlockingQueue<Packet> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        new Server().run();
    }

    private void run() throws IOException, InterruptedException {
        SessionFactory sessionFactory = buildSessionFactory();
        try (ServerSocket serverSocket = new ServerSocket(6666, 0, Inet4Address.getByName("127.0.0.1"))) {
            while (true) {
                log.info("Server start listen on: " + serverSocket.getLocalSocketAddress());
                try (Socket clientSocket = serverSocket.accept()) {
                    log.info("New connection from: " + clientSocket.getRemoteSocketAddress());
                    PacketHandler packetHandler = new PacketHandler("PacketHandler", queue, sessionFactory);
                    packetHandler.start();
                    try {
                        PacketReader packetReader = new PacketReader(new DataInputStream(clientSocket.getInputStream()));
                        while (true) {
                            Packet packet = packetReader.readPacket();
                            queue.add(packet);
                        }
                    } catch (EOFException e) {
                        log.info("Close connection from: " + clientSocket.getRemoteSocketAddress());
                        packetHandler.interrupt();
                        log.info("Wait while PacketHandler thread stop");
                        packetHandler.join();
                        log.info("PacketHandler thread stopped");
                    }
                }
            }
        }
    }

    private SessionFactory buildSessionFactory() {
        Configuration config = new Configuration()
                .configure()
                .addAnnotatedClass(GameCharacter.class)
                .addAnnotatedClass(ItemAuctionInfo.class)
                .addAnnotatedClass(ItemCost.class)
                .addAnnotatedClass(ItemHistory.class)
                .addAnnotatedClass(ItemProfitAction.class)
                .addAnnotatedClass(ItemSource.class)
                .addAnnotatedClass(Spell.class)
                .addAnnotatedClass(TradeHistoryRecord.class);

        StandardServiceRegistry reg = new StandardServiceRegistryBuilder()
                .applySettings(config.getProperties())
                .build();

        return config.buildSessionFactory(reg);
    }
}
