package edu.xtu.bbs.common.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SensitiveWordsDetectorTest {

    private static boolean nativeAvailable;
    private SensitiveWordsDetector detector;

    @BeforeAll
    static void loadNativeAvailability() throws Exception {
        Field field = SensitiveWordsDetector.class.getDeclaredField("NATIVE_AVAILABLE");
        field.setAccessible(true);
        nativeAvailable = field.getBoolean(null);
    }

    @BeforeEach
    void setUp() {
        detector = new SensitiveWordsDetector();
        Assumptions.assumeTrue(nativeAvailable, "Sensitive words native library unavailable; skipping test.");
    }

    @ParameterizedTest(name = "{index} => words={0}, content={1}, expected={2}")
    @MethodSource("matchCases")
    void detectsSensitiveWords(Set<String> words, String content, boolean expected) {
        detector.setSensitiveWords(words);
        boolean actual = detector.containsSensitiveWords(content);
        assertEquals(expected, actual, () -> "Unexpected result for content: " + content);
    }

    private static Collection<Arguments> matchCases() {
        return Arrays.asList(
            Arguments.of(Set.of("forbidden"), "The forbidden topic is here.", true),
            Arguments.of(Set.of("forbidden"), "This sentence is clean.", false),
            Arguments.of(Set.of("敏感词"), "这里包含敏感词，需要拦截。", true),
            Arguments.of(Set.of("敏感词"), "这里都是正常的文字。", false),
            Arguments.of(Set.of("$ecret!", "@@@"), "Symbols like $ecret! should be caught.", true),
            Arguments.of(Set.of("$ecret!", "@@@"), "Triple @@@ signs are also banned.", true),
            Arguments.of(Set.of("$ecret!", "@@@"), "Special characters but not the banned ones.", false),
            Arguments.of(Set.of("forbidden", "敏感词", "$ecret!"), "for bid\nden", true),
            Arguments.of(Set.of("forbidden", "敏感词", "$ecret!"), "敏 \n 感\t词", true),
            Arguments.of(Set.of("forbidden", "敏感词", "$ecret!"), "$ e c r e t !", true),
            Arguments.of(Set.of("forbidden", "敏感词", "$ecret!"), "forbid", false),

            // Emoji related cases
            Arguments.of(Set.of("🔥"), "This is 🔥 hot!", true),
            Arguments.of(Set.of("🔥"), "This is hot!", false),
            Arguments.of(Set.of("emoji🚫"), "This contains emoji🚫 inside.", true),
            Arguments.of(Set.of("🚫"), "Beware of the 🚫 sign.", true),

            // Very long banned word / very long content
            Arguments.of(Set.of("x".repeat(2000)), "prefix " + "x".repeat(2000) + " suffix", true),
            Arguments.of(Set.of("x".repeat(2000)), "prefix " + "x".repeat(199) + " suffix", false),
            Arguments.of(Set.of("longsequence"), "a" + "b".repeat(5000) + " longsequence end", true),

            // Zero-width / obfuscated forms (detector should ignore invisible separators)
            Arguments.of(Set.of("forbidden"), "for\u200Bbidden", true),   // zero-width space inside
            Arguments.of(Set.of("forbidden"), "for\u200Dbidden", true),   // zero-width joiner inside

            // Regex-special characters as literal words
            Arguments.of(Set.of("a+b*c?"), "Sequence a+b*c? appears here.", true),
            Arguments.of(Set.of("a+b*c?"), "a b c ? spaced out", false),

            // Overlapping and repeated patterns
            Arguments.of(Set.of("aaa"), "aaaaa", true),
            Arguments.of(Set.of("repeat"), "repeatrepeatrepeat", true),

            // Empty/edge cases
            Arguments.of(Set.of(), "Some content but no banned words configured.", false),
            Arguments.of(Set.of("forbidden"), "", false)
        );
    }
}
