# eBay Snipe Server

A simple eBay [auction sniping](http://en.wikipedia.org/wiki/Auction_sniping) service that provides a RESTful API for managing scheduled snipes.

It is written in Scala and makes use of a slightly modified version of the [JBidwatcher](https://github.com/cyberfox/jbidwatcher) source code for interacting with eBay.

## Configuration

The server must be given the eBay credentials to be used for bidding on auctions. Create a `src/main/resources/application.conf` file with the following content:

```
ebay {
  username = "<YOUR_EBAY_USERNAME_HERE>"
  password = "<YOUR_EBAY_PASSWORD_HERE>"
}
```

You can look at `src/main/resources/reference.conf` to check for additional server settings you can override.

## Building

The server has [SBT](http://www.scala-sbt.org/) as a build dependency. If you do not have it installed yet and are using a Unix-based OS, I recommend using the [sbt-extras](https://github.com/paulp/sbt-extras) script.

You can start the server directly with SBT by running the `SnipeServer` main class. Alternatively, you can also create a self-contained package by running:

```bash
sbt universal:packageBin
```

A zip file will be created in `target/universal`. It can then be sent to any computer and run by extracting the archive and executing the extracted `bin/ebay-snipe-server` script. Only a JVM installation is required in the production machine.

## API

All the successful responses returned by the server, as well as the entities accepted in POST and PUT requests, are JSON documents. POST and PUT requests must include a proper `Content-Type: application/json` header.

The server currently supports the following HTTP endpoints:

  * **GET /auction/{auctionId}**

  Retrives information about an auction from eBay.

  * **GET /auction/{auctionId}/snipe**

  Returns information about the snipe currently scheduled for an auction.

  * **POST /auction/{auctionId}/snipe**

  Submits information for a snipe to be scheduled for an auction. For scheduling a bid of 0.5 US dollars (the server's default currency), one can simply submit:
  
  ```json
  {
      "bid": 0.5
  }
  ```

  The request can also include the currency to use, a specific time for the snipe to take place and the quantity of items to bid for. If a time is not provided, the server will schedule the snipe for a time it sees fit. The default quantity of items to bid for is 1. The full snipe JSON would be:

  ```json
  {
      "bid": "USD 0.5",
      "quantity": 1,
      "snipeTime": "2014-08-05T02:29:06.690"
  }
  ```

  * **DELETE /auction/{auctionId}/snipe**

  Cancels a previously scheduled snipe for an auction.

  * **GET /snipes**

  Returns all the currently scheduled snipes.

## Copyright

This repository contains a slightly modified version of the [JBidwatcher](https://github.com/cyberfox/jbidwatcher) source code. The license for that code, present in the `src/main/java/jbidwatcher` directory, is available [here](http://www.jbidwatcher.com/home/by-nc-sa-amended). All remaining code is [MIT licensed](http://opensource.org/licenses/MIT).
