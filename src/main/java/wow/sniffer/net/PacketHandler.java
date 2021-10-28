package wow.sniffer.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import wow.sniffer.dao.GameContextDAO;
import wow.sniffer.entity.*;
import wow.sniffer.game.AuctionFaction;
import wow.sniffer.game.AuctionRecord;
import wow.sniffer.game.GameContext;
import wow.sniffer.game.mail.Mail;
import wow.sniffer.game.mail.MailItem;
import wow.sniffer.game.mail.MailType;

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
    private final GameContext gameContext;

    @Autowired
    private GameContextDAO gameContextDAO;

    public PacketHandler() {
        gameContext = new GameContext();
    }

    @Override
    public void run() {
        log.info("Begin game data processing");

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
                        gameContextDAO.saveTradeHistoryRecord(new TradeHistoryRecord(itemId, packet.getTimestamp(), actionString, count, cost));
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

        gameContextDAO.deleteOldItemCostRecords();
        gameContextDAO.deleteOldItemProfitActionRecords();
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
                gameContextDAO.saveItemHistoryList(itemHistoryList);
            }

            // save items prices
            gameContextDAO.saveItemCostList(itemCostList);
            gameContextDAO.deleteOldItemCostRecords();

            // clear profit
            gameContextDAO.deleteAllItemProfitActionRecords();

            // get item prices for profit calc
            List<ItemCost> allItemCostList = gameContextDAO.getAllItemCostList();

            // calc resell profit
            List<ItemProfitAction> itemProfitActions = calculateResellProfit(allItemCostList);
            gameContextDAO.updateItemProfitActionList(itemProfitActions);

            // calc craft profit
            List<Spell> craftSpells = gameContextDAO.getAutoUpdateCraftSpells();
            gameContextDAO.updateItemProfitActionList(calculateProfitListFromCraftSpellList(craftSpells, allItemCostList));

            Instant finish = Instant.now();

            log.info("Auction response package processing took: " + Duration.between(start, finish).toSeconds() + " seconds, updated item count: " + itemCostList.size());
        }
    }

    private List<ItemProfitAction> calculateProfitListFromCraftSpellList(List<Spell> craftSpells, List<ItemCost> itemCostList) {

        List<ItemProfitAction> resultList = new ArrayList<>();

        for (Spell spell : craftSpells) {
            resultList.addAll(calculateProfitListFromCraftSpell(spell, itemCostList));

            for (Spell altSpell : spell.getAltSpellSet()) {
                resultList.addAll(calculateProfitListFromCraftSpell(altSpell, itemCostList));
            }
        }

        return resultList;
    }

    private List<ItemProfitAction> calculateProfitListFromCraftSpell(Spell spell, List<ItemCost> itemCostList) {
        List<ItemProfitAction> resultList = new ArrayList<>();

        // check all component have cost source
        boolean allComponentsCostExist = true;
        for (wow.sniffer.entity.Component component : spell.getComponents()) {
            if (itemCostList.stream().noneMatch(itemCost -> itemCost.getId().equals(component.getItem().getItemId()))) {
                allComponentsCostExist = false;
                break;
            }
        }

        // if craft item or component not have cost source - skip spell
        if (!allComponentsCostExist || itemCostList.stream().noneMatch(itemCost -> itemCost.getId().equals(spell.getCraftItem().getItemId()))) {
            return resultList;
        }

        List<ItemCost> craftItemCostSourceList = itemCostList.stream()
                .filter(itemCost -> itemCost.getId().equals(spell.getCraftItem().getItemId()))
                .sorted(Comparator.comparing(ItemCost::getPrice, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        ItemCost maxCostSource = craftItemCostSourceList.get(0);
        ItemCost allianceItemCost = craftItemCostSourceList.stream().filter(itemCost -> itemCost.getSource().equals("alliance_auction")).findFirst().orElse(null);
        ItemCost hordeItemCost = craftItemCostSourceList.stream().filter(itemCost -> itemCost.getSource().equals("horde_auction")).findFirst().orElse(null);
        Integer allianceMinBuyout = allianceItemCost != null ? allianceItemCost.getPrice() : null;
        Integer hordeMinBuyout = hordeItemCost != null ? hordeItemCost.getPrice() : null;


        StringBuilder spellInfo = new StringBuilder();
        spellInfo.append("spell: (")
                .append(spell.getSpellId())
                .append(") ")
                .append(spell.getName())
                .append("\n");

        StringBuilder sellTo = new StringBuilder();
        sellTo.append("sell to:\n");
        for (ItemCost cost : craftItemCostSourceList) {
            sellTo.append("\t")
                    .append(priceToString(cost.getPrice()))
                    .append(" ")
                    .append(cost.getSource())
                    .append("\n");
        }

        int craftCost = 0;

        StringBuilder compInfo = new StringBuilder();
        compInfo.append("components:\n");
        for (wow.sniffer.entity.Component comp : spell.getComponents()) {

            List<ItemCost> compItemCostSourceList = itemCostList.stream()
                    .filter(itemCost -> itemCost.getId().equals(comp.getItem().getItemId()))
                    .sorted(Comparator.comparing(ItemCost::getPrice))
                    .collect(Collectors.toList());

            craftCost += compItemCostSourceList.get(0).getPrice() * comp.getCount();
            compInfo.append(comp.getCount())
                    .append(" x (")
                    .append(comp.getItem().getItemId())
                    .append(") ")
                    .append(comp.getItem().getName())
                    .append("\n");
            for (ItemCost cost : compItemCostSourceList) {
                compInfo.append("\t")
                        .append(priceToString(cost.getPrice()))
                        .append(" ")
                        .append(cost.getSource())
                        .append("\n");
            }
        }
        // craft cost per item
        craftCost /= spell.getCraftItemCount();
        // profit per item
        int profit = maxCostSource.getPrice() - craftCost;

        spellInfo.append("craft cost: ")
                .append(priceToString(craftCost))
                .append("\n")
                .append("profit: ")
                .append(priceToString(profit))
                .append("\n");

        if (!spell.getSubSpellSet().isEmpty()) {
            spellInfo.append("subspells:\n");
            for (Spell subSpell : spell.getSubSpellSet()) {
                spellInfo.append("\t")
                        .append(subSpell.getName())
                        .append("\n");
            }
        }

        String comment = spellInfo.toString() + "\n" + sellTo.toString() + "\n" + compInfo.toString();

        resultList.add(new ItemProfitAction(spell.getCraftItem().getItemId(), "CRAFT", allianceMinBuyout, hordeMinBuyout, profit, comment, new Date()));

        return resultList;
    }

    private List<ItemProfitAction> calculateResellProfit(List<ItemCost> itemsForCalc) {
        List<ItemProfitAction> resultList = new ArrayList<>();

        Map<Integer, List<ItemCost>> collect = itemsForCalc.stream().collect(Collectors.groupingBy(ItemCost::getId));

        for (Map.Entry<Integer, List<ItemCost>> entry : collect.entrySet()) {
            List<ItemCost> itemCostList = entry.getValue();

            // ignore vendor items
            if (itemCostList.stream().anyMatch(itemCost -> itemCost.getSource().equals("vendor"))) continue;

            itemCostList = itemCostList.stream().filter(itemCost -> !itemCost.getSource().equals("vendor")).collect(Collectors.toList());

            if (itemCostList.isEmpty()) continue;

            if (itemCostList.size() == 1) {
                resultList.addAll(getEmptyAuctionItems(itemCostList));
            } else {
                for (int i = 0; i < itemCostList.size(); i++) {
                    for (int j = i + 1; j < itemCostList.size(); j++) {
                        resultList.addAll(calcProfitFromPair(itemCostList.get(i), itemCostList.get(j)));
                    }
                }
            }
        }

        return resultList;
    }

    private List<ItemProfitAction> getEmptyAuctionItems(List<ItemCost> items) {
        List<ItemProfitAction> resultList = new ArrayList<>();
        ItemCost allianceItemCost = items.stream().filter(itemCost -> itemCost.getSource().equals("alliance_auction")).findFirst().orElse(null);
        ItemCost hordeItemCost = items.stream().filter(itemCost -> itemCost.getSource().equals("horde_auction")).findFirst().orElse(null);

        if (allianceItemCost == null && hordeItemCost == null) {
            throw new IllegalArgumentException("Item has all auctions null item cost");
        }

        String action = allianceItemCost == null ? "EMPTY_ALLIANCE_AUCTION" : "EMPTY_HORDE_AUCTION";

        Integer allianceMinBuyout = allianceItemCost != null ? allianceItemCost.getPrice() : null;
        Integer hordeMinBuyout = hordeItemCost != null ? hordeItemCost.getPrice() : null;

        resultList.add(new ItemProfitAction(items.get(0).getId(), action, allianceMinBuyout, hordeMinBuyout, null, null, new Date()));

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

            Integer allianceMinBuyout = allianceItemCost != null ? allianceItemCost.getPrice() : null;
            Integer hordeMinBuyout = hordeItemCost != null ? hordeItemCost.getPrice() : null;

            String action = "RESELL_" + minItemCost.getSource().toUpperCase() + "_TO_" + maxItemCost.getSource().toUpperCase();
            String comment = minItemCost.getSource() + " " + priceToString(minItemCost.getPrice()) +
                    " -> " + maxItemCost.getSource() + " " + priceToString(maxItemCost.getPrice());

            resultList.add(new ItemProfitAction(minItemCost.getId(), action, allianceMinBuyout, hordeMinBuyout, profit, comment, new Date()));
        }

        return resultList;
    }

    public static String priceToString(Integer price) {
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
        List<Integer> spellIdList = new ArrayList<>();
        packet.skip(1);
        short count = packet.readShortE();
        log.info("Known spells: " + count);

        for (int i = 0; i < count; i++) {
            int spellId = packet.readIntE();
            packet.skip(2);
            spellIdList.add(spellId);
        }

        gameContextDAO.updateGameCharacterSpellIdList(gameContext.getPlayerGUID(), spellIdList);
    }

    private void cmsgPlayerLogin(Packet packet) throws IOException {
        gameContext.setPlayerGUID(packet.readLongE());
    }

    private void smsgEnumCharactersResult(Packet packet) throws IOException {
        log.info("Login chamber character list:");
        List<GameCharacter> charList = new ArrayList<>();
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
            log.info(gameCharacter.toString());
            charList.add(gameCharacter);
        }

        gameContextDAO.updateGameCharacterList(charList);
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
