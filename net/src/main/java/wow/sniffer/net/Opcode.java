package wow.sniffer.net;

public enum Opcode {
    SKIP_PACKET,
    CMSG_AUTH_SESSION,
    SMSG_ENUM_CHARACTERS_RESULT,
    CMSG_PLAYER_LOGIN,
    SMSG_SEND_KNOWN_SPELLS,
    CMSG_AUCTION_LIST_ITEMS,
    SMSG_AUCTION_LIST_RESULT,
    MSG_AUCTION_HELLO,
    SMSG_MAIL_LIST_RESULT,
    SMSG_MAIL_COMMAND_RESULT;

    public static Opcode getOpcodeByCode(int code) {
        switch (code) {
            case 0x01ED: return CMSG_AUTH_SESSION;
            case 0x003B: return SMSG_ENUM_CHARACTERS_RESULT;
            case 0x003D: return CMSG_PLAYER_LOGIN;
            case 0x012A: return SMSG_SEND_KNOWN_SPELLS;
            case 0x0258: return CMSG_AUCTION_LIST_ITEMS;
            case 0x025C: return SMSG_AUCTION_LIST_RESULT;
            case 0x0255: return MSG_AUCTION_HELLO;
            case 0x023B: return SMSG_MAIL_LIST_RESULT;
            case 0x0239: return SMSG_MAIL_COMMAND_RESULT;
        }

        return SKIP_PACKET;
    }
}
