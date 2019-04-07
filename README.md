# eBay Snipe Server

A simple eBay [auction sniping](http://en.wikipedia.org/wiki/Auction_sniping) service that provides a RESTful API for managing scheduled snipes.

It is written in Scala and makes use of the [scala-scraper](https://github.com/ruippeixotog/scala-scraper) library for parsing content and interacting with eBay. Some concepts and ideas are inspired in [JBidwatcher](https://github.com/cyberfox/jbidwatcher).

## Quick Start

The quickest way to get the service up is to use [Docker](https://www.docker.com/). An image is available on [Docker Hub](https://registry.hub.docker.com/u/ruippeixotog/ebay-snipe-server/) and can be started with the following command:

```
docker run -d -p 3647:3647 \
  -e 'EBAY_USERNAME=<your_username>' -e 'EBAY_PASSWORD=<your_password>' \
  ruippeixotog/ebay-snipe-server:0.2.2
```

The web service will be available on port 3647. You can also optionally mount the volumes `/opt/docker/appdata` containing persistent application state and `/opt/docker/logs` containing application logs:

```
docker run -d -p 3647:3647 \
  -e 'EBAY_USERNAME=<your_username>' -e 'EBAY_PASSWORD=<your_password>' \
  -v '<path_to_appdata_in_host>:/opt/docker/appdata' \
  -v '<path_to_logs_in_host>:/opt/docker/logs' \
  ruippeixotog/ebay-snipe-server:0.2.2
```

If you need to change the default configurations or fiddle with the source code, keep reading for instructions on how to build the project.

## Building

The server has [SBT](http://www.scala-sbt.org/) as a build dependency. If you do not have it installed yet and you are using a Unix-based OS, I recommend using the [sbt-extras](https://github.com/paulp/sbt-extras) script.

The server must be given the eBay credentials to be used for bidding on auctions. Create a `src/main/resources/application.conf` file with the following content:

```
ebay {
  username = "<your_username>"
  password = "<your_password>"
}
```

You can look at `src/main/resources/reference.conf` to check for additional server settings you can override.

You can start the server directly with SBT by running the `SnipeServer` main class. Alternatively, you can also create a self-contained package by running:

```bash
sbt universal:packageBin
```

A zip file will be created in `target/universal`. It can then be sent to any computer and run by extracting the archive and executing the extracted `bin/ebay-snipe-server` script. Only a JVM installation is required in the production machine.

## API

All the successful responses returned by the server, as well as the entities accepted in POST and PUT requests, are JSON documents. POST and PUT requests must include a proper `Content-Type: application/json` header.

The server currently supports the following HTTP endpoints:

  * **GET /auction/{auctionId}**

  Retrieves information about an auction from eBay.

  * **GET /auction/{auctionId}/snipe**

  Returns information about the snipe currently scheduled for an auction.

  * **POST /auction/{auctionId}/snipe**

  Submits information for a snipe to be scheduled for an auction. For scheduling a bid of 0.5 US dollars (the server's default currency), one can simply submit:

  ```json
  {
      "bid": 0.5
  }
  ```

  The request can also include the currency to use, a description, a specific time for the snipe to take place and the quantity of items to bid for. If a time is not provided, the server will schedule the snipe for a time it sees fit. The default quantity of items to bid for is 1. The full snipe JSON would be:

  ```json
  {
      "bid": "USD 0.5",
      "description": "cheap and awesome item",
      "quantity": 1,
      "snipeTime": "2014-08-05T02:29:06.690"
  }
  ```

  * **DELETE /auction/{auctionId}/snipe**

  Cancels a previously scheduled snipe for an auction.

  * **GET /snipes**

  Returns all the currently scheduled snipes.

## Copyright

Copyright (c) 2014 Rui Gonçalves. See LICENSE for details.
