
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.JetStream;
import io.nats.client.JetStreamOptions;
import io.nats.client.api.PublishAck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.nio.charset.StandardCharsets;

public class pub_aws {
    // Always create a JSON payload of exactly 1024 bytes with random customer_id field
    public static byte[] buildJsonPayload(int customerId) {
        int payloadSize = 1024;
        String base = "{\"customer_id\":" + customerId + "}";
        int baseLen = base.getBytes(StandardCharsets.UTF_8).length;
        int paddingLen = payloadSize - baseLen - 1;

        StringBuilder json = new StringBuilder("{\"customer_id\":");
        json.append(customerId);

        if (paddingLen > 0) {
            json.append(",\"pad\":\"");
            for (int i = 0; i < paddingLen - 8; i++) {
                json.append('X');
            }
            json.append("\"");
        }
        json.append('}');

        String s = json.toString();
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        if (raw.length < payloadSize) {
            int padPos = json.lastIndexOf("\"}");
            int needed = payloadSize - raw.length;
            for (int i = 0; i < needed; i++) {
                json.insert(padPos, 'X');
            }
            s = json.toString();
        } else if (raw.length > payloadSize) {
            int excess = raw.length - payloadSize;
            String left = json.substring(0, json.length() - excess - 2);
            s = left + "\"}";
        }
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: java pub_aws <subject> <messagesPerSecond> <seconds> <thread_count> <pubBatchSize>");
            return;
        }

        String subject = args[0];
        int messagesPerSecond = Integer.parseInt(args[1]);
        int seconds = Integer.parseInt(args[2]);
        int threadCount = Integer.parseInt(args[3]);
        final int BATCH_SIZE = Integer.parseInt(args[4]);

        if (messagesPerSecond < 1) {
            System.out.println("messagesPerSecond must be greater than 0");
            return;
        }
        if (seconds < 1) {
            System.out.println("seconds must be greater than 0");
            return;
        }
        if (threadCount < 1) {
            System.out.println("thread_count must be greater than 0");
            return;
        }

        final int[] perThreadRate = new int[threadCount];
        int base = messagesPerSecond / threadCount;
        int rem = messagesPerSecond % threadCount;
        for (int i = 0; i < threadCount; i++) {
            perThreadRate[i] = base + (i < rem ? 1 : 0);
        }

        String servers = "nats://172.31.21.241:4222,nats://172.31.16.244:4222,nats://172.31.28.46:4222";
        String username = "user";
        String password = "user";

        String[] serverArray = servers.split(",");
        Options options = new Options.Builder()
                .servers(serverArray)
                .userInfo(username.toCharArray(), password.toCharArray())
                .build();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        final int[] threadStartOffset = new int[threadCount];
        for (int t = 0, offset = 0; t < threadCount; t++) {
            threadStartOffset[t] = offset;
            offset += perThreadRate[t] * seconds;
        }

        try (Connection nc = Nats.connect(options)) {
            JetStream js = nc.jetStream(JetStreamOptions.defaultOptions());

            long totalMsgTarget = (long) messagesPerSecond * seconds;
            System.out.printf(
                    "JetStream (BATCH MODE): Publishing %d messages/sec to subject '%s.<customer_id>' with 1024-byte JSON payload for %d seconds using %d threads (batch size: %d, connected to: %s as user: %s)%n",
                    messagesPerSecond, subject, seconds, threadCount, BATCH_SIZE, servers, username
            );

            CountDownLatch latch = new CountDownLatch(threadCount);
            long startTime = System.currentTimeMillis();
            final long[] published = new long[threadCount];
            final Random[] threadRandom = new Random[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threadRandom[i] = new Random(System.nanoTime() + i * 9973L);
            }

            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                new Thread(() -> {
                    try {
                        long threadTotalPublished = 0;
                        long lastLogged = 0;
                        long threadStartTime = System.currentTimeMillis();
                        int perThreadTotal = perThreadRate[threadIndex] * seconds;
                        int globalThreadOffset = threadStartOffset[threadIndex];
                        Random rand = threadRandom[threadIndex];

                        for (int batchStart = 0; batchStart < perThreadTotal; batchStart += BATCH_SIZE) {
                            int batchSize = Math.min(BATCH_SIZE, perThreadTotal - batchStart);
                            List<CompletableFuture<PublishAck>> futures = new ArrayList<>(batchSize);

                            for (int i = 0; i < batchSize; i++) {
                                // Generate a random customer_id for each message
                                int customerId = 1 + rand.nextInt(1_000_000);
                                String msgSubject = subject + "." + customerId;
                                byte[] payloadBytes = buildJsonPayload(customerId);

                                futures.add(js.publishAsync(msgSubject, payloadBytes));
                            }

                            for (CompletableFuture<PublishAck> fut : futures) {
                                try {
                                    fut.get();
                                } catch (Exception e) {
                                    System.err.printf("Thread %d: Publish ACK error: %s%n", threadIndex, e.getMessage());
                                }
                            }

                            threadTotalPublished += batchSize;
                            published[threadIndex] = threadTotalPublished;

                            long elapsed = (System.currentTimeMillis() - threadStartTime) / 1000;
                            if (threadIndex == 0 && elapsed / 30 > lastLogged / 30) {
                                long totalPublished = 0;
                                for (long n : published) totalPublished += n;
                                String now = LocalDateTime.now().format(dtf);
                                System.out.printf("[%s] Batch-mode: Published %d messages at %d seconds%n", now, totalPublished, elapsed);
                                lastLogged = elapsed;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();
            long now = System.currentTimeMillis();
            String ts = LocalDateTime.now().format(dtf);
            long totalPublished = 0;
            for (long n : published) totalPublished += n;
            System.out.printf("[%s] Publishing complete. Total messages published: %d%n", ts, totalPublished);
            long duration = (now - startTime) / 1000;
            System.out.printf("Total elapsed time: %d seconds%n", duration);
        }
    }
}

