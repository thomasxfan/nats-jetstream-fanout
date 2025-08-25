# nats-jetstream-fanout
Example showing high fan-out with NATS JetStream using subject-based partitioning to scale delivery to millions of customers/apps.

**Problem:** Relaying a high volume of backend events to browsers/mobile clients requires scalable consumption and delivery without dropping messages.

**Goal:** Offer a runnable example using JetStream streams, durable consumers, and subject-based partitions to horizontally scale consumption.
Provide a minimal reference where JetStream stores events and partitioned gateway instances fan out to clients via WebSocket.

**Architecture Diagram**


# Quick Start

**Setup nats**\
Include config/shared.cfg to nats configuration file. Use subject based partitioning to shard the data\
Start nats server
   
**create 100 streams**\
../scripts/createStreamAll.sh 100 

**create all consumers**\
// create 100 consumers for each of 3 webservers, total 300 consumers\
../scripts/createConAll.sh

**Other utils.**\
// purge data across the 100 streams\
../scripts/purgeStream.sh 100
// Delete all the consumers\
../scripts/deleteConAll.sh
// Delete all the streams\
../scripts/deletStr.sh 100


**Java apps**\
// start webserver 1, with 100 workers each work call 1 consumer\
../scripts/runApp1.sh

// start webserver 2, with 100 workers each work call 1 consumer\
../scripts/runApp2.sh

// start webserver 3, with 100 workers each work call 1 consumer\
../scripts/runApp3.sh

// start publisher, pub 5000 tps * 600 (seconds) messages in total.\
// Adjust the thread count and batch size as needed to increase the tps\
java -cp ".:../java/libs/*" pub_aws user.messages 5000 600 10 50

// Stop all the webservers\
../scripts/stopApps.sh



