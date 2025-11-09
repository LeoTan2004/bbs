package edu.xtu.bbs.common.trace;

import edu.xtu.bbs.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TraceId Feature Demo Controller
 * Used to demonstrate the usage of request tracing functionality
 */
@RestController
@RequestMapping("/api/trace")
@Slf4j
public class TraceTestController {
    
    /**
     * Test TraceId functionality
     */
    @GetMapping("/test")
    public ApiResponse<String> testTrace(@RequestParam(defaultValue = "Hello TraceId") String message) {
        log.info("Processing trace test request with message: {}", message);
        
        // Simulate some business logic
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String traceId = TraceIdUtil.getTraceId();
        log.info("Current TraceId: {}", traceId);
        
        return ApiResponse.success("Response: " + message + ", TraceId: " + traceId);
    }
    
    /**
     * Test TraceId in exception scenarios
     */
    @GetMapping("/test-error")
    public ApiResponse<String> testTraceWithError() {
        log.info("Processing trace test error request");
        
        // Intentionally throw exception to test TraceId in exception handling
        throw new RuntimeException("This is a test exception for TraceId");
    }
    
    /**
     * Get current TraceId
     */
    @GetMapping("/current-trace-id")
    public ApiResponse<String> getCurrentTraceId() {
        String traceId = TraceIdUtil.getTraceId();
        log.info("Returning current TraceId: {}", traceId);
        return ApiResponse.success(traceId);
    }
}