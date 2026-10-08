package kernel.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UuidV7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID id = UuidV7.generate();
        assertEquals(7, id.version());
        assertEquals(2, id.variant());
    }

    @Test
    void embedsCurrentTimestamp() {
        long before = System.currentTimeMillis();
        UUID id = UuidV7.generate();
        long ts = id.getMostSignificantBits() >>> 16;
        assertTrue(ts >= before - 1 && ts <= System.currentTimeMillis() + 10);
    }

    @Test
    void isStrictlyIncreasingInSingleThread() {
        UUID previous = UuidV7.generate();
        for (int i = 0; i < 20_000; i++) {
            UUID next = UuidV7.generate();
            // so sánh msb dạng không dấu: timestamp 48 bit nên msb luôn dương
            assertTrue(next.getMostSignificantBits() >= previous.getMostSignificantBits());
            previous = next;
        }
    }

    @Test
    void isUniqueAcrossThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<List<UUID>>> futures = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                futures.add(pool.submit(() -> {
                    List<UUID> ids = new ArrayList<>();
                    for (int i = 0; i < 5_000; i++) ids.add(UuidV7.generate());
                    return ids;
                }));
            }
            Set<UUID> all = new HashSet<>();
            for (Future<List<UUID>> f : futures) all.addAll(f.get());
            assertEquals(40_000, all.size());
        } finally {
            pool.shutdown();
        }
    }
}
