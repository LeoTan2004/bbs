package edu.xtu.bbs.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceIdUtil Test Class
 */
class TraceIdUtilTest {

    @BeforeEach
    void setUp() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate unique TraceId")
    void shouldGenerateUniqueTraceId() {
        String traceId1 = TraceIdUtil.generateTraceId();
        String traceId2 = TraceIdUtil.generateTraceId();
        
        assertNotNull(traceId1);
        assertNotNull(traceId2);
        assertNotEquals(traceId1, traceId2);
        assertEquals(32, traceId1.length()); // UUID without hyphens
        assertEquals(32, traceId2.length());
    }

    @Test
    @DisplayName("Should set and get TraceId correctly")
    void shouldSetAndGetTraceId() {
        String testTraceId = "test123456789";
        
        // Initially should be null
        assertNull(TraceIdUtil.getTraceId());
        
        // Set TraceId
        TraceIdUtil.setTraceId(testTraceId);
        
        // Should return the set TraceId
        assertEquals(testTraceId, TraceIdUtil.getTraceId());
    }

    @Test
    @DisplayName("Should clear TraceId correctly")
    void shouldClearTraceId() {
        String testTraceId = "test123456789";
        
        // Set TraceId
        TraceIdUtil.setTraceId(testTraceId);
        assertEquals(testTraceId, TraceIdUtil.getTraceId());
        
        // Clear TraceId
        TraceIdUtil.clearTraceId();
        
        // Should be null after clearing
        assertNull(TraceIdUtil.getTraceId());
    }

    @Test
    @DisplayName("Should handle null and empty TraceId")
    void shouldHandleNullAndEmptyTraceId() {
        // Test null
        TraceIdUtil.setTraceId(null);
        assertNull(TraceIdUtil.getTraceId());
        
        // Test empty string
        TraceIdUtil.setTraceId("");
        assertNull(TraceIdUtil.getTraceId());
        
        // Test whitespace
        TraceIdUtil.setTraceId("   ");
        assertNull(TraceIdUtil.getTraceId());
    }

    @Test
    @DisplayName("Should get or generate TraceId correctly")
    void shouldGetOrGenerateTraceId() {
        // Should generate new TraceId when none exists
        String traceId1 = TraceIdUtil.getOrGenerateTraceId();
        assertNotNull(traceId1);
        assertEquals(32, traceId1.length());
        
        // Should return the same TraceId on subsequent calls
        String traceId2 = TraceIdUtil.getOrGenerateTraceId();
        assertEquals(traceId1, traceId2);
        
        // Clear and generate new one
        TraceIdUtil.clearTraceId();
        String traceId3 = TraceIdUtil.getOrGenerateTraceId();
        assertNotNull(traceId3);
        assertNotEquals(traceId1, traceId3);
    }

    @Test
    @DisplayName("Should clear all MDC data")
    void shouldClearAllMDCData() {
        // Set some test data in MDC
        MDC.put("testKey", "testValue");
        TraceIdUtil.setTraceId("testTraceId");
        
        // Verify data exists
        assertEquals("testValue", MDC.get("testKey"));
        assertEquals("testTraceId", TraceIdUtil.getTraceId());
        
        // Clear all
        TraceIdUtil.clearAll();
        
        // All should be null
        assertNull(MDC.get("testKey"));
        assertNull(TraceIdUtil.getTraceId());
    }

    @Test
    @DisplayName("Should use correct constants")
    void shouldUseCorrectConstants() {
        assertEquals("traceId", TraceIdUtil.TRACE_ID_KEY);
        assertEquals("X-Trace-Id", TraceIdUtil.TRACE_ID_HEADER);
    }
}