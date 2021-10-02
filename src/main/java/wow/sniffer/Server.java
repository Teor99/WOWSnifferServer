package wow.sniffer;

import wow.sniffer.net.PacketHandler;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public void listen(int port) throws IOException {
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("new connection found");
                    System.out.println();
                    PacketHandler packetHandler = new PacketHandler(new DataInputStream(clientSocket.getInputStream()));
                    packetHandler.processInputStream();
                } catch (EOFException ignored) {
                    System.err.println("connection closed");
                    System.err.println();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().listen(6666);
    }
}
