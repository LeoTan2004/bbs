package edu.xtu.bbs.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logging Aspect
 * Unified handling of method call logs and exception logging
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Define pointcut: all public methods in Controller layer
     */
    @Pointcut("execution(public * edu.xtu.bbs.*.controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * Define pointcut: all public methods in Service layer
     */
    @Pointcut("execution(public * edu.xtu.bbs.*.service..*.*(..))")
    public void serviceMethods() {
    }

    /**
     * Around advice - Controller layer methods
     */
    @Around("controllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Mask sensitive information
        Object[] maskedArgs = maskSensitiveData(args);

        log.info("Controller method started: {}.{}, args: {}",
                className, methodName, Arrays.toString(maskedArgs));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();

            log.info("Controller method completed: {}.{}, duration: {}ms",
                    className, methodName, (endTime - startTime));

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();

            log.error("Controller method failed: {}.{}, duration: {}ms, error: {}",
                    className, methodName, (endTime - startTime), e.getClass().getSimpleName(), e);

            throw e;
        }
    }

    /**
     * Around advice - Service layer methods
     */
    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // For Service layer methods, use DEBUG level logging
        if (log.isDebugEnabled()) {
            Object[] maskedArgs = maskSensitiveData(args);
            log.debug("Service method started: {}.{}, args: {}",
                    className, methodName, Arrays.toString(maskedArgs));
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug("Service method completed: {}.{}, duration: {}ms",
                        className, methodName, (endTime - startTime));
            }

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();

            // Service layer exceptions use WARN level logging
            log.warn("Service method failed: {}.{}, duration: {}ms, error: {}",
                    className, methodName, (endTime - startTime), e.getClass().getSimpleName(), e);

            throw e;
        }
    }

    /**
     * Exception advice - Log all uncaught exceptions
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Object[] maskedArgs = maskSensitiveData(args);

        log.error("Uncaught exception in {}.{}, args: {}, exception: {}",
                className, methodName, Arrays.toString(maskedArgs), ex.getMessage(), ex);
    }

    /**
     * Mask sensitive information
     */
    private Object[] maskSensitiveData(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }

        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof String strArg) {
                // Mask emails
                if (strArg.contains("@")) {
                    maskedArgs[i] = strArg.replaceAll("(.{1,3}).*(@.*)", "$1***$2");
                } else if (isPassword(strArg)) {
                    // Mask passwords
                    maskedArgs[i] = "***";
                } else {
                    maskedArgs[i] = arg;
                }
            } else if (arg != null && isUserRelatedObject(arg)) {
                // For user-related objects, convert to simplified string
                maskedArgs[i] = arg.getClass().getSimpleName() + "@" + Integer.toHexString(arg.hashCode());
            } else {
                maskedArgs[i] = arg;
            }
        }
        return maskedArgs;
    }

    /**
     * Check if it's a password field
     */
    private boolean isPassword(String str) {
        return str != null && (str.length() > 6 && str.length() < 100) &&
                !str.contains("@") && !str.contains(" ");
    }

    /**
     * Check if it's a user-related object
     */
    private boolean isUserRelatedObject(Object obj) {
        return obj.getClass().getPackage() != null &&
                obj.getClass().getPackage().getName().startsWith("edu.xtu.bbs");
    }
}