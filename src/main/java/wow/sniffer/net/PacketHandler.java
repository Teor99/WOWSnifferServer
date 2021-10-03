package wow.sniffer.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import wow.sniffer.Utils;
import wow.sniffer.game.AuctionRecord;
import wow.sniffer.game.Character;
import wow.sniffer.game.GameContext;
import wow.sniffer.game.ItemStat;
import wow.sniffer.repos.ItemStatRepository;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static wow.sniffer.net.Opcode.*;

@Component
public class PacketHandler {
    @Autowired
    private ItemStatRepository itemStatRepository;

    private DataInputStream dis;
    private GameContext gameContext;

    public PacketHandler() {
    }

    public void processInputStream(DataInputStream dis) throws IOException {
        System.out.println("Begin of data stream processing");
        this.dis = dis;
        gameContext = new GameContext();
        while (true) {
            try {
                Packet packet = readPacket();
                handlePacket(packet);
            } catch (EOFException e) {
                System.err.println("End of data stream processing");
                break;
            }
        }
    }

    private Packet readPacket() throws IOException {
        int packetOpcode = Utils.readIntReverted(dis);
        int packetSize = Utils.readIntReverted(dis);
        int timestamp = Utils.readIntReverted(dis);
        byte packetType = dis.readByte();
        byte[] packetData = null;

        if (packetSize > 0) {
            packetData = new byte[packetSize];
            for (int i = 0; i < packetData.length; i++) {
                packetData[i] = dis.readByte();
            }
        }

        gameContext.setTimestamp(timestamp);
        return new Packet(packetOpcode, packetSize, timestamp, packetType, packetData);
    }

    private void handlePacket(Packet packet) {

        try {
            if (packet.getOpcode() == CMSG_AUTH_SESSION.opcode) {
                cmsgAuthSession(packet);
            } else if (packet.getOpcode() == SMSG_ENUM_CHARACTERS_RESULT.opcode) {
                smsgEnumCharactersResult(packet);
            } else if (packet.getOpcode() == CMSG_PLAYER_LOGIN.opcode) {
                cmsgPlayerLogin(packet);
            } else if (packet.getOpcode() == SMSG_SEND_KNOWN_SPELLS.opcode) {
                smsgSendKnownSpells(packet);
            } else if (packet.getOpcode() == CMSG_AUCTION_LIST_ITEMS.opcode) {
                cmsgAuctionListItems(packet);
            } else if (packet.getOpcode() == SMSG_AUCTION_LIST_RESULT.opcode) {
                smsgAuctionListResult(packet);
            } else if (packet.getOpcode() == MSG_AUCTION_HELLO.opcode && packet.getType() == MsgType.ServerToClient.ordinal()) {
                smsgAuctionHello(packet);
            }
        } catch (IOException e) {
            System.err.println(packet);
            e.printStackTrace();
        }
    }

    private void cmsgAuctionListItems(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        int savedAuctionRecords = Utils.readIntReverted(dis);
        if (savedAuctionRecords == 0) {
            gameContext.getAuctionRecords().clear();
        } else if (savedAuctionRecords != gameContext.getAuctionRecords().size()) {
            System.err.println("auction records count not equals: context - "
                    + gameContext.getAuctionRecords().size()
                    + " expected - " + savedAuctionRecords);
        }
    }

    private void smsgAuctionHello(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        int ahId = Utils.readIntReverted(dis);

        gameContext.setAuctionHouseId(ahId);

        System.out.println();
        String descr = getAuctionHouseDescription(ahId);
        System.out.println("Auction house id: " + ahId + " (" + descr + ")");
    }

    private String getAuctionHouseDescription(int ahId) {
        switch (ahId) {
            case 1:
                return "Alliance";
            case 2:
                return "Alliance Ironforge";
            case 5:
                return "Horde Thunder Bluff";
            default:
                System.err.println("Unknown auction house id: " + ahId);
        }

        return null;
    }

    private void smsgAuctionListResult(Packet packet) throws IOException {
//        System.out.println();
//        System.out.println("Auction result list func begin, game context records: " + gameContext.getAuctionRecords().size());
        DataInputStream dis = packet.getDataInputStream();
        int count = Utils.readIntReverted(dis);
        if (count == 0) return;

        for (int i = 0; i < count; i++) {
            dis.skip(4);
            int id = Utils.readIntReverted(dis);
            dis.skip(92);
            int itemCount = Utils.readIntReverted(dis);
            dis.skip(24);
            int buyout = Utils.readIntReverted(dis);
            dis.skip(16);
            gameContext.getAuctionRecords().add(new AuctionRecord(id, packet.getTimestamp(), itemCount, buyout, gameContext.getFaction()));
        }

        int totalItemCount = Utils.readIntReverted(dis);
        if (totalItemCount == 0) {
            throw new IllegalArgumentException("Auction list result total count 0, but count: " + count);
        }
        dis.skip(4);

        if (totalItemCount == gameContext.getAuctionRecords().size()) {
            List<ItemStat> itemStatList = getItemStatListFromAuctionRecordsList(gameContext.getAuctionRecords());
            System.out.println();
            System.out.println("New item stat found(" + itemStatList.size() + "):");
            itemStatList.forEach(System.out::println);
            itemStatRepository.saveAll(itemStatList);
        }
    }

    private List<ItemStat> getItemStatListFromAuctionRecordsList(List<AuctionRecord> auctionRecords) {
        List<AuctionRecord> tmpList = new ArrayList<>(auctionRecords);
        List<ItemStat> resultList = new ArrayList<>();

        while (!tmpList.isEmpty()) {
            List<AuctionRecord> listToRemove = new ArrayList<>();
            AuctionRecord auctionRecord = tmpList.get(0);
            ItemStat itemStat = new ItemStat(auctionRecord.getId());
            for (AuctionRecord ar : tmpList) {
                if (itemStat.getId() == ar.getId()) {
                    // total count
                    itemStat.setTotalCount(itemStat.getTotalCount() + ar.getCount());
                    // auction count
                    itemStat.setAuctionCount(itemStat.getAuctionCount() + 1);
                    // min buyout
                    if (itemStat.getMinBuyout() == 0) {
                        itemStat.setMinBuyout(ar.getBuyoutPerItem());
                    } else if (ar.getBuyoutPerItem() != 0) {
                        itemStat.setMinBuyout(Math.min(itemStat.getMinBuyout(), ar.getBuyoutPerItem()));
                    }
                    // timestamp
                    itemStat.setTimestamp(new Date((long)auctionRecord.getTimestamp() * 1000));
                    // faction
                    itemStat.setFaction(gameContext.getFaction());

                    listToRemove.add(ar);
                }
            }

            tmpList.removeAll(listToRemove);
            listToRemove.clear();
            resultList.add(itemStat);
        }

        return resultList;
    }

    private void smsgSendKnownSpells(Packet packet) throws IOException {
        List<Integer> spellList = new ArrayList<>();
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(1);
        short count = Utils.readShortReverted(dis);
        for (int i = 0; i < count; i++) {
            int spellId = Utils.readIntReverted(dis);
            dis.skip(2);
            spellList.add(spellId);
        }

        gameContext.getCharacter().setSpellList(spellList);

        System.out.println();
        System.out.println("Known spells(" + spellList.size() + "):");
        System.out.println(spellList);
    }

    private void cmsgPlayerLogin(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        long guid = Utils.readLongReverted(dis);
        for (Character character : gameContext.getLoginChamberCharList()) {
            if (character.getGuid() == guid) {
                gameContext.setCharacter(character);
                System.out.println();
                System.out.println("Login with character:");
                System.out.println(character);
                return;
            }
        }

        throw new IllegalArgumentException("not found character with guid: " + Long.toHexString(guid));
    }

    private void smsgEnumCharactersResult(Packet packet) throws IOException {
        List<Character> charList = new ArrayList<>();

        System.out.println();
        System.out.println("Login chamber character list:");

        DataInputStream dis = packet.getDataInputStream();
        byte count = dis.readByte();
        for (int i = 0; i < count; i++) {
            long guid = Utils.readLongReverted(dis);
            String charName = Utils.readCString(dis);
            byte raceCode = dis.readByte();
            byte classCode = dis.readByte();
            dis.skip(6);
            byte level = dis.readByte();
            dis.skip(252);
            Character character = new Character(guid, charName, raceCode, classCode, level);
            System.out.println(character);
            charList.add(character);
        }

        gameContext.setLoginChamberCharList(charList);
    }

    private void cmsgAuthSession(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        gameContext.setAccountName(Utils.readCString(dis));
        System.out.println();
        System.out.println("Client auth request with account: " + gameContext.getAccountName());
    }
}
