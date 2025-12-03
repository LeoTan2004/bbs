package edu.xtu.bbs.user.config;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Registers GraalVM runtime hints required by the Spring Security infrastructure
 * when building a native image. Without these hints the generated executable
 * fails to create the JDK dynamic proxy that wraps the AuthenticationManager
 * during application startup.
 */
class SecurityRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.proxies().registerJdkProxy(
                AuthenticationManager.class,
                MessageSourceAware.class,
                InitializingBean.class,
                SpringProxy.class,
                Advised.class,
                DecoratingProxy.class
        );
    }
}
