package co.elastic.tutorial.javamonitor;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;

/**
 * Three endpoints matching the tutorial's scenario: a normal request,
 * an artificially slow request, and a request that fails until the
 * reader fixes the underlying issue.
 */
@Controller
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private final InventoryService inventoryService;

    public DemoController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Normal request: GET /greeting?item=mug
    @GetMapping("/greeting")
    public String greeting(@RequestParam(name = "item", defaultValue = "mug") String item, Model model) {
        logger.info("Handling greeting request for item [{}]", item);
        int stock = inventoryService.getStock(item);
        model.addAttribute("item", item);
        model.addAttribute("stock", stock);
        return "greeting";
    }

    // Artificially slow request: GET /slow?item=mug
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> slow(
            @RequestParam(name = "item", defaultValue = "mug") String item) throws InterruptedException {
        int stock = inventoryService.getStockFromWarehouse(item);
        return ResponseEntity.ok(Map.of("item", item, "stock", stock, "source", "warehouse"));
    }

    // Failing request until PRICING_SERVICE_ENABLED=true: GET /error?item=mug
    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> error(
            @RequestParam(name = "item", defaultValue = "mug") String item) {
        double price = inventoryService.getPrice(item);
        return ResponseEntity.ok(Map.of("item", item, "price", price));
    }

    @ExceptionHandler(InventoryService.PricingServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handlePricingServiceUnavailable(
            InventoryService.PricingServiceUnavailableException ex) {
        logger.error("Returning 503 for pricing lookup failure", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }
}
