# WOW Sniffer Server
### For World of Warcraft: Wrath of the Lich King v3.3.5 12340 (tested on private server)
The application is designed to process packets between WOW Client and WOW Server and save information from the Auction House in the MySQL database. The data is used for analysis, statistics collection and use in the profitable buying / selling of items, calculating the cost of crafting items.

### Used projects
As injection sniffer used https://github.com/Anubisss/SzimatSzatyor. It can store packets as binary file and text (human readable log). Project has been modified, added functionality of dump packet data to TCP Socket connection.
<br>Also, when writing the server, was used https://github.com/TrinityCore/WowPacketParser for parse bin files and fetch packet structure.

### Build
The project is in prototype state and starts directly from IntelliJ IDEA 2020.3.1 (Community Edition)
<br>OS: Windows 10

### Run process:
 - Starts WOWSnifferServer, it begin listen localhost:6666
 - run wow.exe (WOW WOTLK Client)
 - on login screen run sniffer(modified SzimatSzatyor), it inject to client and start TCP connection to WOWSnifferServer
 - login in to server, select character, go to Auction House and start request items/buy/sell, all data been saved to DB. 

### Features
 - Process response packages from the Auction House and keep the minimum price for the item. Both single item requests and full scan packages (55,000 items) are supported.
Screenshot of how it is stored in the database:
<p align="center"><img src="https://user-images.githubusercontent.com/5261564/140885963-4cc35b53-6374-494e-bd30-e5aa9c1e1332.png"/></p>

 - Calculation of profit when reselling or crafting an item. Also, a comment is compiled for each entry, which contains the components and their cost.
<p align="center"><img src="https://user-images.githubusercontent.com/5261564/140887266-703b049c-b2ed-4998-aaa9-a5d9d895dd7f.png"/></p>


 - Saving the history of the price of an item, for statistics. For example: minimum price, median price per month / week / day.
They are saved simultaneously with the upper table.
<p align="center"><img src="https://user-images.githubusercontent.com/5261564/140886112-fd8b33bf-8edd-4f03-8006-90400f7b54a0.png"/></p>


 - Saving information about purchases / sales in the Auction House for keeping statistics of purchase prices of materials, their quantity, counting the money earned from the sale of goods, what goods, their quantity. Information is taken from the mailbox when the letter(with money or items) is picked up.
<p align="center"><img src="https://user-images.githubusercontent.com/5261564/140885806-8ed3cbaf-e831-47b1-819e-8a08f397f68c.png"/></p>
