package co.elastic.tutorial.javamonitor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A tiny in-memory "inventory" backend used to give the three demo endpoints
 * something to call. The pricing lookup is disabled by default, which is the
 * bug the tutorial asks readers to find and fix.
 */
@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private static final Map<String, Integer> STOCK = Map.of(
            "mug", 42,
            "t-shirt", 17,
            "sticker-pack", 128
    );

    // Toggled on purpose: this is the bug the tutorial walks through finding and fixing.
    // Flip it to true (or set PRICING_SERVICE_ENABLED=true) once you've diagnosed /error.
    private final AtomicBoolean pricingServiceEnabled =
            new AtomicBoolean(Boolean.parseBoolean(System.getenv().getOrDefault("PRICING_SERVICE_ENABLED", "false")));

    public int getStock(String item) {
        logger.info("Checking stock for item [{}]", item);
        return STOCK.getOrDefault(item, 0);
    }

    /**
     * Simulates a slow downstream warehouse lookup. Real latency, not a sleep
     * hidden behind a flag, so the trace duration is genuinely variable.
     */
    public int getStockFromWarehouse(String item) throws InterruptedException {
        int delayMillis = 800 + (int) (Math.random() * 2200);
        logger.info("Querying warehouse system for item [{}], expecting ~{}ms", item, delayMillis);
        Thread.sleep(delayMillis);
        return getStock(item);
    }

    /**
     * Simulates calling a pricing service that's misconfigured/unreachable
     * until PRICING_SERVICE_ENABLED is set to true.
     */
    public double getPrice(String item) {
        if (!pricingServiceEnabled.get()) {
            logger.error("Pricing service lookup failed for item [{}]: service not enabled", item);
            throw new PricingServiceUnavailableException(
                    "Pricing service is unavailable (PRICING_SERVICE_ENABLED=false)");
        }
        logger.info("Fetched price for item [{}]", item);
        return switch (item) {
            case "mug" -> 12.50;
            case "t-shirt" -> 19.99;
            case "sticker-pack" -> 4.00;
            default -> 9.99;
        };
    }

    public static class PricingServiceUnavailableException extends RuntimeException {
        public PricingServiceUnavailableException(String message) {
            super(message);
        }
    }
}
