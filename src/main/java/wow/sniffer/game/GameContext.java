package wow.sniffer.game;

import wow.sniffer.game.mail.Mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameContext {
    private String accountName;
    private List<Character> loginChamberCharList;
    private Character character;
    private final List<AuctionRecord> auctionRecords = new ArrayList<>();
    private Date timestamp;
    private AuctionFaction auctionFaction;
    private List<Mail> mailList;

    public Character getCharacter() {
        return character;
    }

    public List<AuctionRecord> getAuctionRecords() {
        return auctionRecords;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public AuctionFaction getAuctionFaction() {
        return auctionFaction;
    }

    public void setAuctionFaction(AuctionFaction auctionFaction) {
        this.auctionFaction = auctionFaction;
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

    public List<Mail> getMailList() {
        return mailList;
    }

    public void setMailList(List<Mail> mailList) {
        this.mailList = mailList;
    }
}
