package edu.xtu.bbs.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId Management Utility
 * Used to generate and manage request tracking IDs, supports SLF4J MDC integration
 */
public class TraceIdUtil {
    
    /**
     * TraceId key name in MDC
     */
    public static final String TRACE_ID_KEY = "traceId";
    
    /**
     * TraceId key name in HTTP request headers
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    
    /**
     * Generate new TraceId
     * 
     * @return newly generated TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Set TraceId for current thread
     * 
     * @param traceId TraceId to be set
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }
    
    /**
     * Get TraceId of current thread
     * 
     * @return TraceId of current thread, returns null if not set
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * Clear TraceId of current thread
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
    
    /**
     * Clear all MDC data of current thread
     */
    public static void clearAll() {
        MDC.clear();
    }
    
    /**
     * Get or generate TraceId
     * Returns existing TraceId if current thread has one, otherwise generates a new one
     * 
     * @return TraceId
     */
    public static String getOrGenerateTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }
}