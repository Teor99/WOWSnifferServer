package wow.sniffer.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import wow.sniffer.entity.*;
import wow.sniffer.game.AuctionFaction;
import wow.sniffer.game.AuctionRecord;
import wow.sniffer.game.GameContext;
import wow.sniffer.game.mail.Mail;
import wow.sniffer.game.mail.MailItem;
import wow.sniffer.game.mail.MailType;
import wow.sniffer.repo.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PacketHandler extends Thread {

    private final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private BlockingQueue<Packet> queue;

    @Autowired
    private ItemCostRepository itemCostRepository;
    @Autowired
    private GameCharacterRepository gameCharacterRepository;
    @Autowired
    private SpellRepository spellRepository;
    @Autowired
    private ItemHistoryRepository itemHistoryRepository;
    @Autowired
    private TradeHistoryRecordRepository tradeHistoryRecordRepository;
    @Autowired
    private ItemProfitActionRepository itemProfitActionRepository;

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

        itemCostRepository.removeOldRecords();
        itemProfitActionRepository.removeOldRecords();
    }

    private void smsgAuctionListResult(Packet packet) throws IOException {
        Instant start = Instant.now();
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

        boolean isFullScan = totalItemCount > 1000;

        if (totalItemCount == gameContext.getAuctionRecords().size()) {
            List<ItemCost> itemCostList = getItemStatListFromAuctionRecordsList(gameContext.getAuctionRecords());

            // save item price history if fullscan
            if (isFullScan) {
                List<ItemHistory> itemHistoryList = new ArrayList<>();
                for (ItemCost itemCost : itemCostList) {
                    itemHistoryList.add(new ItemHistory(itemCost.getId(), itemCost.getPrice(), packet.getTimestamp()));
                }
                itemHistoryRepository.saveAll(itemHistoryList);
            }

            // save items prices
            itemCostRepository.saveAll(itemCostList);
            itemCostRepository.removeOldRecords();

            // Profit
            if (isFullScan) {
                itemProfitActionRepository.deleteAll();
                ArrayList<ItemCost> allItemCostList = new ArrayList<>();
                itemCostRepository.findAll().forEach(allItemCostList::add);
                itemProfitActionRepository.saveAll(calculateProfit(allItemCostList));
            } else {
                Set<Integer> set = new HashSet<>();
                itemCostList.forEach(itemCost -> set.add(itemCost.getId()));
                itemProfitActionRepository.deleteAllByItemId(new ArrayList<>(set));
                itemProfitActionRepository.saveAll(calculateProfit(itemCostRepository.findAllByItemId(new ArrayList<>(set))));
            }

            itemProfitActionRepository.removeOldRecords();

            Instant finish = Instant.now();

            log.info("Auction response package processing took: " + Duration.between(start, finish).toSeconds() + " seconds, updated item count: " + itemCostList.size());
        }
    }

    private List<ItemProfitAction> calculateProfit(List<ItemCost> itemsForCalc) {
        List<ItemProfitAction> resultList = new ArrayList<>();

        Map<Integer, List<ItemCost>> collect = itemsForCalc.stream().collect(Collectors.groupingBy(ItemCost::getId));

        for (Map.Entry<Integer, List<ItemCost>> entry : collect.entrySet()) {
            List<ItemCost> itemCostList = entry.getValue();

            if (itemCostList.size() == 1 && itemCostList.stream().allMatch(itemCost -> itemCost.getSource().equals("vendor"))) {
                continue;
            }

            if (itemCostList.size() > 1) {
                for (int i = 0; i < itemCostList.size(); i++) {
                    for (int j = i + 1; j < itemCostList.size(); j++) {
                        resultList.addAll(calcProfitFromPair(itemCostList.get(i), itemCostList.get(j)));
                    }
                }
            }
        }

        return resultList;
    }

    private List<ItemProfitAction> calcProfitFromPair(ItemCost firstItemCost, ItemCost secondItemCost) {
        List<ItemProfitAction> resultList = new ArrayList<>();
        List<ItemCost> items = new ArrayList<>();
        items.add(firstItemCost);
        items.add(secondItemCost);
        items.sort(Comparator.comparing(ItemCost::getPrice));

        ItemCost minItemCost = items.get(0);
        ItemCost maxItemCost = items.get(1);

        int profit = maxItemCost.getPrice() - minItemCost.getPrice();
        if (profit >= 0) {
            ItemCost allianceItemCost = items.stream().filter(itemCost -> itemCost.getSource().equals("alliance_auction")).findFirst().orElse(null);
            ItemCost hordeItemCost = items.stream().filter(itemCost -> itemCost.getSource().equals("horde_auction")).findFirst().orElse(null);

            Integer allianceMinBuyout = allianceItemCost != null? allianceItemCost.getPrice() : null;
            Integer hordeMinBuyout = hordeItemCost != null? hordeItemCost.getPrice() : null;

            String action = "RESELL_" + minItemCost.getSource().toUpperCase() + "_TO_" + maxItemCost.getSource().toUpperCase();
            String comment = minItemCost.getSource() + " " + costToString(minItemCost.getPrice()) +
                    " -> " + maxItemCost.getSource() + " " + costToString(maxItemCost.getPrice());

            resultList.add(new ItemProfitAction(minItemCost.getId(), action, allianceMinBuyout, hordeMinBuyout, profit, comment, new Date()));
        }

        return resultList;
    }

    public static String costToString(Integer price) {
        int cooper = price % 100;
        int silver = (price - cooper) % 10000 / 100;
        int gold = (price - silver - cooper) / 10000;
        StringBuilder sb = new StringBuilder();
        if (gold != 0) {
            sb.append(gold).append("g");
        }
        if (silver != 0) {
            sb.append(silver).append("s");
        }
        if (cooper != 0) {
            sb.append(cooper).append("c");
        }

        return sb.toString();
    }


    private List<ItemCost> getItemStatListFromAuctionRecordsList(List<AuctionRecord> auctionRecords) {
        List<ItemCost> resultList = new ArrayList<>();
        if (auctionRecords.isEmpty()) return resultList;

        auctionRecords.sort(Comparator.comparing(AuctionRecord::getId));

        String source = null;
        if (gameContext.getAuctionFaction() == AuctionFaction.ALLIANCE) {
            source = "alliance_auction";
        } else if (gameContext.getAuctionFaction() == AuctionFaction.HORDE) {
            source = "horde_auction";
        }

        Map<Integer, List<AuctionRecord>> collect = auctionRecords.stream().collect(Collectors.groupingBy(AuctionRecord::getId));

        for (Map.Entry<Integer, List<AuctionRecord>> entry : collect.entrySet()) {
            List<AuctionRecord> groupedAuctionRecords = entry.getValue();
            AuctionRecord auctionRecord = groupedAuctionRecords.stream()
                    .filter(record -> record.getBuyoutPerItem() != 0)
                    .min(Comparator.comparing(AuctionRecord::getBuyoutPerItem)).orElse(null);

            if (auctionRecord != null) {
                resultList.add(new ItemCost(new ItemSource(auctionRecord.getId(), source), auctionRecord.getBuyoutPerItem(), new Date()));
            }
        }

        return resultList;
    }

    private void smsgSendKnownSpells(Packet packet) throws IOException {
        Set<Spell> spellSet = new HashSet<>();
        packet.skip(1);
        short count = packet.readShortE();
        log.info("Known spells: " + count);

        for (int i = 0; i < count; i++) {
            int spellId = packet.readIntE();
            packet.skip(2);
            spellRepository.findById(spellId).ifPresent(spellSet::add);
        }

        log.info("spell set from db: " + spellSet.size());
        gameContext.getCharacter().setSpellSet(spellSet);
        gameCharacterRepository.save(gameContext.getCharacter());
    }

    private void cmsgPlayerLogin(Packet packet) throws IOException {
        long guid = packet.readLongE();
        for (GameCharacter gameCharacter : gameContext.getLoginChamberCharList()) {
            if (gameCharacter.getId() == guid) {
                gameContext.setCharacter(gameCharacter);
                log.info("Login with character:");
                log.info(String.valueOf(gameCharacter));
                return;
            }
        }

        throw new IllegalArgumentException("not found character with guid: " + Long.toHexString(guid));
    }

    private void smsgEnumCharactersResult(Packet packet) throws IOException {
        List<GameCharacter> charList = new ArrayList<>();

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
            GameCharacter gameCharacter = new GameCharacter(guid, charName);
            log.info(String.valueOf(gameCharacter));
            charList.add(gameCharacter);
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
