# nats-jetstream-fanout
Example showing high fan-out with NATS JetStream using subject-based partitioning to scale delivery to millions of customers/apps.

**Problem:** Relaying a high volume of backend events to browsers/mobile clients requires scalable consumption and delivery without dropping messages.

**Goal:** Offer a runnable example using JetStream streams, durable consumers, and subject-based partitions to horizontally scale consumption.
Provide a minimal reference where JetStream stores events and partitioned gateway instances fan out to clients via WebSocket.

# Quick Start

**Setup nats**
1) Include config/shared.cfg to your nats configuration file. Use subject based partitioning to shard the data
2) Start nats server

3) ../scripts/createStreamAll.sh 100 // create 100 streams
4) ../scripts/createConAll.sh // create 100 consumers for each of 3 webservers, total 300 consumers

Other utils.
../scripts/purgeStream.sh 100   //purge data across the 100 streams


**Java apps**
5) ./runApp1.sh // start webserver 1
6) 


