package wow.sniffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wow.sniffer.net.PacketHandler;

import java.io.DataInputStream;
import java.io.EOFException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

@SpringBootApplication
public class Server implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(Server.class);

    @Autowired
    private PacketHandler packetHandler;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(6666, 0, Inet4Address.getByName("127.0.0.1"))) {
            while (true) {
                log.info("Server start listen on: " + serverSocket.getLocalSocketAddress());
                try (Socket clientSocket = serverSocket.accept()) {
                    log.info("New connection from: " + clientSocket.getRemoteSocketAddress());
                    try {
                        packetHandler.processInputStream(new DataInputStream(clientSocket.getInputStream()));
                    } catch (EOFException e) {
                        log.warn("Close connection from: " + clientSocket.getRemoteSocketAddress());
                    }
                }
            }
        }
    }
}
