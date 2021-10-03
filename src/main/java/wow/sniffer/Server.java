package wow.sniffer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wow.sniffer.net.PacketHandler;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//@EnableAutoConfiguration
@SpringBootApplication
public class Server implements CommandLineRunner {

    @Autowired
    private PacketHandler packetHandler;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(Server.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(6666)) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("new connection found");
                    System.out.println();
                    packetHandler.processInputStream(new DataInputStream(clientSocket.getInputStream()));
                } catch (EOFException ignored) {
                    System.err.println("connection closed");
                    System.err.println();
                }
            }
        }
    }
}
