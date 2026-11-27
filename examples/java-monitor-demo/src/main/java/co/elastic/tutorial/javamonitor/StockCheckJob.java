package co.elastic.tutorial.javamonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job that produces a steady trickle of log lines, so there's
 * always something in Discover besides request logs.
 */
@Component
public class StockCheckJob {

    private static final Logger logger = LoggerFactory.getLogger(StockCheckJob.class);
    private static final String[] ITEMS = {"mug", "t-shirt", "sticker-pack"};

    private final InventoryService inventoryService;

    public StockCheckJob(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedDelay = 15000)
    public void checkStockLevels() {
        for (String item : ITEMS) {
            int stock = inventoryService.getStock(item);
            if (stock < 20) {
                logger.warn("Stock low for item [{}]: {} remaining", item, stock);
            } else {
                logger.info("Stock check: item [{}] has {} remaining", item, stock);
            }
        }
    }
}
