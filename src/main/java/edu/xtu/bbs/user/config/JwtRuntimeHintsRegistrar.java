package edu.xtu.bbs.user.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


/**
 * Registers JWT related classes that are loaded via reflection so that the GraalVM
 * native-image build retains them. Without this hint {@code KeysBridge} is removed
 * and the native executable fails during startup.
 */
class JwtRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints,@Nullable ClassLoader classLoader) {
        hints.reflection().registerType(
                TypeReference.of("io.jsonwebtoken.impl.security.KeysBridge"),
                MemberCategory.values());
    }
}
