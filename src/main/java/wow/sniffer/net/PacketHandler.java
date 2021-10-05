package wow.sniffer.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import wow.sniffer.Utils;
import wow.sniffer.game.AuctionFaction;
import wow.sniffer.game.AuctionRecord;
import wow.sniffer.game.Character;
import wow.sniffer.game.GameContext;
import wow.sniffer.game.entity.ItemAuctionInfo;
import wow.sniffer.game.entity.ItemHistory;
import wow.sniffer.game.entity.ItemStat;
import wow.sniffer.game.entity.TradeHistoryRecord;
import wow.sniffer.game.mail.Mail;
import wow.sniffer.game.mail.MailItem;
import wow.sniffer.game.mail.MailType;
import wow.sniffer.repos.ItemHistoryRepository;
import wow.sniffer.repos.ItemStatRepository;
import wow.sniffer.repos.TradeHistoryRecordRepository;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PacketHandler {

    private final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    @Autowired
    private ItemStatRepository itemStatRepository;
    @Autowired
    private ItemHistoryRepository itemHistoryRepository;
    @Autowired
    private TradeHistoryRecordRepository tradeHistoryRecordRepository;

    private DataInputStream dis;
    private GameContext gameContext;

    public PacketHandler() {
    }

    public void processInputStream(DataInputStream dis) throws IOException {
        log.info("Begin of data stream processing");
        this.dis = dis;
        gameContext = new GameContext();
        while (true) {
            try {
                Packet packet = readPacket();
                handlePacket(packet);
            } catch (EOFException e) {
                log.warn("End of data stream processing");
                throw e;
            }
        }
    }

    private Packet readPacket() throws IOException {
        int packetOpcode = Utils.readIntReverted(dis);
        int packetSize = Utils.readIntReverted(dis);
        Date timestamp = new Date((long) Utils.readIntReverted(dis) * 1000);
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
            Opcode opcode = Opcode.getOpcodeByCode(packet.getOpcode());
            switch (opcode) {
                case CMSG_AUTH_SESSION:
                    cmsgAuthSession(packet);
                    break;
                case SMSG_ENUM_CHARACTERS_RESULT:
                    smsgEnumCharactersResult(packet);
                    break;
                case CMSG_PLAYER_LOGIN:
                    cmsgPlayerLogin(packet);
                    break;
                case SMSG_SEND_KNOWN_SPELLS:
                    smsgSendKnownSpells(packet);
                    break;
                case CMSG_AUCTION_LIST_ITEMS:
                    cmsgAuctionListItems(packet);
                    break;
                case SMSG_AUCTION_LIST_RESULT:
                    smsgAuctionListResult(packet);
                    break;
                case MSG_AUCTION_HELLO:
                    if (packet.getType() == Direction.ServerToClient.ordinal()) smsgAuctionHello(packet);
                    break;
                case SMSG_MAIL_LIST_RESULT:
                    smsgMailListResult(packet);
                    break;
                case SMSG_MAIL_COMMAND_RESULT:
                    smsgMailCommandResult(packet);
                    break;
            }
        } catch (IOException e) {
            log.warn(String.valueOf(packet));
            e.printStackTrace();
        }

    }

    private void smsgMailCommandResult(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        int mailId = Utils.readIntReverted(dis);
        int actionCode = Utils.readIntReverted(dis);
        int errorCode = Utils.readIntReverted(dis);

//        log.info("mailId: " + mailId + " action: " + actionCode + " error: " + errorCode);

        if (actionCode == 4 && errorCode == 0) {
            Mail mailForRemove = gameContext.getMailList().stream().filter(mail -> mail.getId() == mailId).collect(Collectors.toList()).stream().findFirst().orElse(null);
            if (mailForRemove == null) {
                log.error("Not found mailId(" + mailId + ") in mail list");
            } else {
                if (mailForRemove.getMailType() == MailType.AUCTION) {
                    String[] subjectArray = mailForRemove.getSubject().split(":");
                    String[] bodyArray = mailForRemove.getBody().split(":");
                    int itemId = Integer.parseInt(subjectArray[0]);
                    int action = Integer.parseInt(subjectArray[2]);
                    int count = Integer.parseInt(subjectArray[4]);
                    int cost = Integer.parseInt(bodyArray[2]) / count;
                    String actionString = null;
                    if (action == 1) {
                        actionString = "buy";
                    } else if (action == 2) {
                        actionString = "sell";
                    }

                    if (actionString != null) {
                        tradeHistoryRecordRepository.save(new TradeHistoryRecord(itemId, packet.getTimestamp(), actionString, count, cost));
                    }
                }


                gameContext.getMailList().remove(mailForRemove);
            }
        }

    }

    private void smsgMailListResult(Packet packet) throws IOException {
        List<Mail> mailList = new ArrayList<>();
        DataInputStream dis = packet.getDataInputStream();
        int totalMailCount = Utils.readIntReverted(dis);
        byte mailCount = dis.readByte();
        for (int i = 0; i < mailCount; i++) {
            short msgSize = Utils.readShortReverted(dis);
            int mailId = Utils.readIntReverted(dis);
            MailType mailType = MailType.getMailTypeByCode(dis.readByte());
            long playerGUID = 0;
            int entry = 0;
            switch (mailType) {
                case NORMAL:
                    playerGUID = Utils.readLongReverted(dis);
                    break;
                case CREATURE:
                case GAMEOBJECT:
                case ITEM:
                case AUCTION:
                    entry = Utils.readIntReverted(dis);
                    break;
            }
            int cod = Utils.readIntReverted(dis);
            int packageId = Utils.readIntReverted(dis);
            int stationery = Utils.readIntReverted(dis);
            int money = Utils.readIntReverted(dis);
            int flags = Utils.readIntReverted(dis);
            int time = Utils.readIntReverted(dis);
            int templateId = Utils.readIntReverted(dis);
            String subject = Utils.readCString(dis);
            String body = Utils.readCString(dis).trim();

            int packageItemCount = dis.readByte();
            List<MailItem> attachedItems = new ArrayList<>();
            for (int j = 0; j < packageItemCount; j++) {
                byte itemIndex = dis.readByte();
                int guid = Utils.readIntReverted(dis);
                int itemId = Utils.readIntReverted(dis);
                for (int k = 0; k < 7; k++) {
                    dis.skip(12);
                }
                dis.skip(8);
                int itemCount = Utils.readIntReverted(dis);
                dis.skip(13);
                attachedItems.add(new MailItem(guid, itemId, itemCount));
            }
            mailList.add(new Mail(mailId, msgSize, mailType, playerGUID, entry, cod, packageId, stationery, money, flags,
                    time, templateId, subject, body, attachedItems));
        }

        gameContext.setMailList(mailList);
    }

    private void cmsgAuctionListItems(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        int savedAuctionRecords = Utils.readIntReverted(dis);
        if (savedAuctionRecords == 0) {
            gameContext.getAuctionRecords().clear();
        } else if (savedAuctionRecords != gameContext.getAuctionRecords().size()) {
            log.warn("auction records count not equals: context - "
                    + gameContext.getAuctionRecords().size()
                    + " expected - " + savedAuctionRecords);
        }
    }

    private void smsgAuctionHello(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        int ahId = Utils.readIntReverted(dis);

        gameContext.setAuctionFaction(AuctionFaction.getFactionByCode(ahId));

        log.info("Set auction faction as: " + gameContext.getAuctionFaction().name());
    }

    private void smsgAuctionListResult(Packet packet) throws IOException {
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
            gameContext.getAuctionRecords().add(new AuctionRecord(id, packet.getTimestamp(), itemCount, buyout));
        }

        int totalItemCount = Utils.readIntReverted(dis);
        if (totalItemCount == 0) {
            throw new IllegalArgumentException("Auction list result total count 0, but count: " + count);
        }
        dis.skip(4);

        if (totalItemCount == gameContext.getAuctionRecords().size()) {
            List<ItemStat> itemStatList = getItemStatListFromAuctionRecordsList(gameContext.getAuctionRecords());
            log.info("Update item stat, count: " + itemStatList.size());
            List<ItemHistory> itemHistoryList = new ArrayList<>();
            for (ItemStat itemStat : itemStatList) {
                Integer minBuyout = (gameContext.getAuctionFaction() == AuctionFaction.ALLIANCE) ?
                        itemStat.getAllianceAuctionInfo().getMinBuyout() : itemStat.getHordeAuctionInfo().getMinBuyout();

                itemHistoryList.add(new ItemHistory(itemStat.getId(), minBuyout, new Timestamp(gameContext.getTimestamp().getTime())));
            }

            itemHistoryRepository.saveAll(itemHistoryList);
            itemStatRepository.saveAll(itemStatList);
            itemStatRepository.removeOldRecords();
        }
    }


    private List<ItemStat> getItemStatListFromAuctionRecordsList(List<AuctionRecord> auctionRecords) {
        List<AuctionRecord> tmpList = new ArrayList<>(auctionRecords);
        List<ItemStat> resultList = new ArrayList<>();

        while (!tmpList.isEmpty()) {
            List<AuctionRecord> listToRemove = new ArrayList<>();
            AuctionRecord auctionRecord = tmpList.get(0);

            ItemStat itemStat = itemStatRepository.findById(auctionRecord.getId()).orElse(new ItemStat(auctionRecord.getId()));
            ItemAuctionInfo itemAuctionInfo = new ItemAuctionInfo();

            if (gameContext.getAuctionFaction() == AuctionFaction.ALLIANCE) {
                itemStat.setAllianceAuctionInfo(itemAuctionInfo);
            } else {
                itemStat.setHordeAuctionInfo(itemAuctionInfo);
            }

            // init
            itemAuctionInfo.setTotalCount(0);
            itemAuctionInfo.setMinBuyout(0);
            itemAuctionInfo.setAuctionCount(0);

            for (AuctionRecord ar : tmpList) {
                if (itemStat.getId() == ar.getId()) {
                    // total count
                    itemAuctionInfo.setTotalCount(itemAuctionInfo.getTotalCount() + ar.getCount());
                    // auction count
                    itemAuctionInfo.setAuctionCount(itemAuctionInfo.getAuctionCount() + 1);
                    // min buyout
                    if (itemAuctionInfo.getMinBuyout() == 0) {
                        itemAuctionInfo.setMinBuyout(ar.getBuyoutPerItem());
                    } else if (ar.getBuyoutPerItem() != 0) {
                        itemAuctionInfo.setMinBuyout(Math.min(itemAuctionInfo.getMinBuyout(), ar.getBuyoutPerItem()));
                    }
                    // timestamp
                    itemAuctionInfo.setTimestamp(auctionRecord.getTimestamp());

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

        log.info("Known spells: " + spellList.size());
    }

    private void cmsgPlayerLogin(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        long guid = Utils.readLongReverted(dis);
        for (Character character : gameContext.getLoginChamberCharList()) {
            if (character.getGuid() == guid) {
                gameContext.setCharacter(character);
                log.info("Login with character:");
                log.info(String.valueOf(character));
                return;
            }
        }

        throw new IllegalArgumentException("not found character with guid: " + Long.toHexString(guid));
    }

    private void smsgEnumCharactersResult(Packet packet) throws IOException {
        List<Character> charList = new ArrayList<>();

        log.info("Login chamber character list:");

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
            log.info(String.valueOf(character));
            charList.add(character);
        }

        gameContext.setLoginChamberCharList(charList);
    }

    private void cmsgAuthSession(Packet packet) throws IOException {
        DataInputStream dis = packet.getDataInputStream();
        dis.skip(8);
        gameContext.setAccountName(Utils.readCString(dis));
        log.info("Client auth request with account: " + gameContext.getAccountName());
    }
}
