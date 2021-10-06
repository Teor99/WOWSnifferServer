package wow.sniffer.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PacketHandler extends Thread {

    private final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private BlockingQueue<Packet> queue;

    @Autowired
    private ItemStatRepository itemStatRepository;
    @Autowired
    private ItemHistoryRepository itemHistoryRepository;
    @Autowired
    private TradeHistoryRecordRepository tradeHistoryRecordRepository;

    private GameContext gameContext;

    public PacketHandler() {
    }

    @Override
    public void run() {
        log.info("Begin game data processing");
        gameContext = new GameContext();

        try {
            while (!isInterrupted()) {
                Packet packet = queue.take();
                handlePacket(packet);
            }
        } catch (InterruptedException ignored) {
        }
        log.info("End of game data processing");
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
            log.error(String.valueOf(packet));
            e.printStackTrace();
        }

    }

    private void smsgMailCommandResult(Packet packet) throws IOException {
        int mailId = packet.readIntE();
        int actionCode = packet.readIntE();
        int errorCode = packet.readIntE();

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
        int totalMailCount = packet.readIntE();
        byte mailCount = packet.readByte();
        for (int i = 0; i < mailCount; i++) {
            short msgSize = packet.readShortE();
            int mailId = packet.readIntE();
            MailType mailType = MailType.getMailTypeByCode(packet.readByte());
            long playerGUID = 0;
            int entry = 0;
            switch (mailType) {
                case NORMAL:
                    playerGUID = packet.readLongE();
                    break;
                case CREATURE:
                case GAMEOBJECT:
                case ITEM:
                case AUCTION:
                    entry = packet.readIntE();
                    break;
            }
            int cod = packet.readIntE();
            int packageId = packet.readIntE();
            int stationery = packet.readIntE();
            int money = packet.readIntE();
            int flags = packet.readIntE();
            int time = packet.readIntE();
            int templateId = packet.readIntE();
            String subject = packet.readCString();
            String body = packet.readCString().trim();

            int packageItemCount = packet.readByte();
            List<MailItem> attachedItems = new ArrayList<>();
            for (int j = 0; j < packageItemCount; j++) {
                byte itemIndex = packet.readByte();
                int guid = packet.readIntE();
                int itemId = packet.readIntE();
                for (int k = 0; k < 7; k++) {
                    packet.skip(12);
                }
                packet.skip(8);
                int itemCount = packet.readIntE();
                packet.skip(13);
                attachedItems.add(new MailItem(guid, itemId, itemCount));
            }
            mailList.add(new Mail(mailId, msgSize, mailType, playerGUID, entry, cod, packageId, stationery, money, flags,
                    time, templateId, subject, body, attachedItems));
        }

        gameContext.setMailList(mailList);
    }

    private void cmsgAuctionListItems(Packet packet) throws IOException {
        packet.skip(8);
        int savedAuctionRecords = packet.readIntE();
        if (savedAuctionRecords == 0) {
            gameContext.getAuctionRecords().clear();
        } else if (savedAuctionRecords != gameContext.getAuctionRecords().size()) {
            log.info("auction records count not equals: context - "
                    + gameContext.getAuctionRecords().size()
                    + " expected - " + savedAuctionRecords);
        }
    }

    private void smsgAuctionHello(Packet packet) throws IOException {
        packet.skip(8);
        int ahId = packet.readIntE();

        gameContext.setAuctionFaction(AuctionFaction.getFactionByCode(ahId));

        log.info("Set auction faction as: " + gameContext.getAuctionFaction().name());
    }

    private void smsgAuctionListResult(Packet packet) throws IOException {
        int count = packet.readIntE();
        if (count == 0) return;

        for (int i = 0; i < count; i++) {
            packet.skip(4);
            int id = packet.readIntE();
            packet.skip(92);
            int itemCount = packet.readIntE();
            packet.skip(24);
            int buyout = packet.readIntE();
            packet.skip(16);
            gameContext.getAuctionRecords().add(new AuctionRecord(id, packet.getTimestamp(), itemCount, buyout));
        }

        int totalItemCount = packet.readIntE();
        if (totalItemCount == 0) {
            throw new IllegalArgumentException("Auction list result total count 0, but count: " + count);
        }
        packet.skip(4);

        if (totalItemCount == gameContext.getAuctionRecords().size()) {
            List<ItemStat> itemStatList = getItemStatListFromAuctionRecordsList(gameContext.getAuctionRecords());
            log.info("Update item stat, count: " + itemStatList.size());
            List<ItemHistory> itemHistoryList = new ArrayList<>();
            for (ItemStat itemStat : itemStatList) {
                Integer minBuyout = (gameContext.getAuctionFaction() == AuctionFaction.ALLIANCE) ?
                        itemStat.getAllianceAuctionInfo().getMinBuyout() : itemStat.getHordeAuctionInfo().getMinBuyout();

                itemHistoryList.add(new ItemHistory(itemStat.getId(), minBuyout, packet.getTimestamp()));
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
        packet.skip(1);
        short count = packet.readShortE();
        for (int i = 0; i < count; i++) {
            int spellId = packet.readIntE();
            packet.skip(2);
            spellList.add(spellId);
        }

        gameContext.getCharacter().setSpellList(spellList);

        log.info("Known spells: " + spellList.size());
    }

    private void cmsgPlayerLogin(Packet packet) throws IOException {
        long guid = packet.readLongE();
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

        byte count = packet.readByte();
        for (int i = 0; i < count; i++) {
            long guid = packet.readLongE();
            String charName = packet.readCString();
            byte raceCode = packet.readByte();
            byte classCode = packet.readByte();
            packet.skip(6);
            byte level = packet.readByte();
            packet.skip(252);
            Character character = new Character(guid, charName, raceCode, classCode, level);
            log.info(String.valueOf(character));
            charList.add(character);
        }

        gameContext.setLoginChamberCharList(charList);
    }

    private void cmsgAuthSession(Packet packet) throws IOException {
        packet.skip(8);
        gameContext.setAccountName(packet.readCString());
        log.info("Client auth request with account: " + gameContext.getAccountName());
    }

    public BlockingQueue<Packet> getQueue() {
        return queue;
    }

    public void setQueue(BlockingQueue<Packet> queue) {
        this.queue = queue;
    }


}
