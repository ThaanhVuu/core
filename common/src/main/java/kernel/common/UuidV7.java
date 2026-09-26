package kernel.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Sinh UUID phiên bản 7 theo RFC 9562.
 *
 * <p>UUID gồm 48 bit timestamp (mili giây), 12 bit bộ đếm và 62 bit ngẫu nhiên, nên các giá trị
 * sinh ra trong cùng một JVM tăng dần theo thời gian. Điều này giúp chúng phù hợp làm khoá chính
 * vì giảm phân mảnh chỉ mục.
 *
 * <p>Lưu ý: UUID lộ thời điểm tạo, không nên dùng làm token bí mật.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Timestamp (mili giây) của UUID sinh gần nhất, có thể vượt trước đồng hồ thật. */
    private static long lastTimestamp = -1;
    /** Bộ đếm 12 bit trong cùng một mili giây. */
    private static long counter = 0;

    private UuidV7() {
    }

    /**
     * Sinh một UUID v7 mới.
     *
     * <p>Các UUID sinh ra trong cùng một JVM luôn tăng dần, kể cả khi sinh nhiều giá trị trong cùng
     * mili giây hoặc khi đồng hồ hệ thống bị lùi. Khi đó timestamp trong UUID có thể lệch
     * vài mili giây so với thời gian thực. Phương thức này an toàn khi dùng đa luồng.
     *
     * @return UUID phiên bản 7
     */
    public static synchronized UUID generate() {
        long now = System.currentTimeMillis();

        if (now > lastTimestamp) {
            // Sang mili giây mới: bộ đếm bắt đầu từ một số ngẫu nhiên nhỏ
            lastTimestamp = now;
            counter = RANDOM.nextInt(0x800);
        } else {
            // Cùng mili giây (hoặc đồng hồ bị lùi): tăng bộ đếm để giữ thứ tự
            counter++;
            if (counter > 0xFFF) {
                // Hết 12 bit của bộ đếm: mượn sang mili giây kế tiếp
                lastTimestamp++;
                counter = 0;
            }
        }

        long msb = (lastTimestamp << 16)   // 48 bit timestamp
                | (0x7L << 12)            // 4 bit version = 7
                | counter;                // 12 bit rand_a (bộ đếm)

        long lsb = (0x2L << 62)                                  // 2 bit variant = 10
                | (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL); // 62 bit ngẫu nhiên

        return new UUID(msb, lsb);
    }
}