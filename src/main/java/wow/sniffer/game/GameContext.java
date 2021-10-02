package wow.sniffer.game;

import java.util.ArrayList;
import java.util.List;

public class GameContext {
    private String accountName;
    private List<Character> loginChamberCharList;
    private Character character;
    private final List<AuctionRecord> auctionRecords = new ArrayList<>();
    private int timestamp;
    private int auctionHouseId;

    public Character getCharacter() {
        return character;
    }

    public List<AuctionRecord> getAuctionRecords() {
        return auctionRecords;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getAuctionHouseId() {
        return auctionHouseId;
    }

    public void setAuctionHouseId(int auctionHouseId) {
        this.auctionHouseId = auctionHouseId;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public List<Character> getLoginChamberCharList() {
        return loginChamberCharList;
    }

    public void setLoginChamberCharList(List<Character> loginChamberCharList) {
        this.loginChamberCharList = loginChamberCharList;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
