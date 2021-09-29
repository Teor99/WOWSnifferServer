package wow.sniffer;

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

                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    while (true) {
                        Packet packet = readPacket(dis);
//                        handlePacket(packet);
                        System.out.println(packet);
                        System.out.println();
                    }
                } catch (EOFException ignored) {
                    System.err.println("connection closed");
                    System.err.println();
                }
            }
        }
    }

    private void handlePacket(Packet packet) {

    }


    private Packet readPacket(DataInputStream dis) throws IOException {

        int packetOpcode = Utils.revertInt(dis.readInt());
        int packetSize = Utils.revertInt(dis.readInt());
        int timestamp = Utils.revertInt(dis.readInt());
        byte packetType = dis.readByte();
        byte[] packetData = null;

        if (packetSize > 0) {
            packetData = new byte[packetSize];
            for (int i = 0; i < packetData.length; i++) {
                packetData[i] = dis.readByte();
            }
        }

        return new Packet(packetOpcode, packetSize, timestamp, packetType, packetData);
    }

    public static void main(String[] args) throws IOException {
        new Server().listen(6666);
    }
}
