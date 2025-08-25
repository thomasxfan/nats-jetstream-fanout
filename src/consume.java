import io.nats.client.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class consume {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java consume <startClientId> <endClientId> <appname> <numberOfWorkers>");
            return;
        }
        int startClientId, endClientId, numberOfWorkers;
        String appName;
        try {
            startClientId = Integer.parseInt(args[0]);
            endClientId = Integer.parseInt(args[1]);
            appName = args[2];
            numberOfWorkers = Integer.parseInt(args[3]);
            if (startClientId > endClientId || numberOfWorkers < 1) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            System.out.println("Invalid arguments.");
            return;
        }

        String servers = "nats://172.31.21.241:4222,nats://172.31.16.244:4222,nats://172.31.28.46:4222";
        String[] serverArray = servers.split(",");
        String username = "user";
        String password = "user";

        // Build lookup table for client IDs
        Map<Integer, String> clientLookup = new HashMap<>();
        for (int clientId = startClientId; clientId <= endClientId; clientId++) {
            clientLookup.put(clientId, "Client_" + clientId);
        }

        // Atomic counter for processed messages (thread safe)
        AtomicLong totalProcessed = new AtomicLong();

        // ObjectMapper for JSON parsing
        ObjectMapper objectMapper = new ObjectMapper();

        // Connect to NATS
        Options options = new Options.Builder()
                .servers(serverArray)
                .userInfo(username.toCharArray(), password.toCharArray())
                .build();

        Connection nc = Nats.connect(options);
        JetStream js = nc.jetStream();

        System.out.println("Connected to NATS servers: " + servers);
        System.out.println("Starting workers...");

        int fetchSize = 200;
        int expiryTimeMS = 500;

        for (int workerNum = 1; workerNum <= numberOfWorkers; workerNum++) {
            final int wNum = workerNum;
            new Thread(() -> {
                String consumerName = appName + "_" + "p" + wNum + "_con";
                String streamName = "p" + wNum;
                try {
                    PullSubscribeOptions optionsSub = PullSubscribeOptions.builder()
                            .stream(streamName)
                            .durable(consumerName)
                            .build();
                    JetStreamSubscription sub = js.subscribe("", optionsSub);

                    while (true) {
                        try {
                            Iterable<Message> messages = sub.fetch(fetchSize, Duration.ofMillis(expiryTimeMS));
                            int count = 0;
                            for (Message msg : messages) {
                                count++;
                                boolean acked = false;
                                try {
                                    String payload = new String(msg.getData());
                                    JsonNode root = objectMapper.readTree(payload);
                                    JsonNode customerIdNode = root.get("customer_id");
                            
                                    if (customerIdNode != null && customerIdNode.isInt()) {
                                        int customerId = customerIdNode.intValue();
                                        System.out.printf(
                                                "Worker %d: with customer_id=%d.%n",
                                                wNum, customerId);

                                        if (clientLookup.containsKey(customerId)) {
                                            msg.ack();
                                            acked = true;
                                            System.out.printf(
                                                "Worker %d: Acked message with customer_id=%d.%n",
                                                wNum, customerId);

                                            // Delete the message from the stream, using its sequence number
                                            /*
                                            MessageInfo msgInfo = msg.metaData();
                                            if (msgInfo != null) {
                                                long seq = msgInfo.sequence();
                                                boolean deleted = js.deleteMessage(streamName, seq);
                                                if (deleted) {
                                                    System.out.printf(
                                                        "Worker %d: Deleted message seq=%d from stream %s.%n",
                                                        wNum, seq, streamName);
                                                } else {
                                                    System.out.printf(
                                                        "Worker %d: Failed to delete message seq=%d from stream %s.%n",
                                                        wNum, seq, streamName);
                                                }
                                            } else {
                                                System.out.printf(
                                                    "Worker %d: No message metadata for deletion.%n",
                                                    wNum);
                                            }
                                            */
                                          //  totalProcessed.incrementAndGet(); // count processed
                                        }
                                        else {
                                            msg.term(); //user not in the list, don't deliver this message again
                                        }
                                        totalProcessed.incrementAndGet(); // count processed

                                    }
                                } catch (Exception jsonEx) {
                                    System.out.printf("Worker %d: JSON parsing error - %s%n", wNum, jsonEx.getMessage());
                                }
                                if (!acked) {
                                    System.out.printf(
                                        "Worker %d: Message not acked (no matching customer_id).%n", wNum);
                                }
                            }
                     //       if (count == 0) {
                     //           System.out.printf("Worker %d: No messages received this batch.%n", wNum);
                     //       }
                        } catch (Exception fetchEx) {
                            System.out.printf("Worker %d: Fetch error - %s%n", wNum, fetchEx.getMessage());
                        }
                        try {
                            Thread.sleep(100); // Wait 100 ms before fetching next batch
                        } catch (InterruptedException ie) {
                            System.out.printf("Worker %d interrupted, exiting.%n", wNum);
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("Worker %d: Error subscribing or fetching - %s%n", wNum, e.getMessage());
                }
            }).start();
        }

        // Periodically print processed count every 30 seconds
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        while (true) {
            Thread.sleep(30_000);
            String timestamp = LocalDateTime.now().format(formatter);
            System.out.println("[" + timestamp + "] Total messages processed (acked and termed) so far: " + totalProcessed.get());
        }
    }
}
