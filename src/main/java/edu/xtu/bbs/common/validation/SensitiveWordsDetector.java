package edu.xtu.bbs.common.validation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SensitiveWordsDetector {

    private static final String LIB_NAME = "sensitive_words_detector";
    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary(LIB_NAME);
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            loaded = false;
            log.warn("Failed to load sensitive words native library via java.library.path.", e);
            try {
                Path devLibrary = Path.of("target", "native", System.mapLibraryName(LIB_NAME));
                if (Files.exists(devLibrary)) {
                    System.load(devLibrary.toAbsolutePath().toString());
                    loaded = true;
                    log.info("Loaded sensitive words native library from {}", devLibrary);
                }
            } catch (UnsatisfiedLinkError | SecurityException inner) {
                log.error("Failed to load sensitive words native library from development path.", inner);
            }
        }
        NATIVE_AVAILABLE = loaded;
    }

    /**
     * Set sensitive words.
     *
     * @apiNote Words are persisted to a temporary UTF-8 encoded file before the
     *          native matcher loads them, so inputs must be encoded consistently
     *          with the application text.
     * @param words the sensitive words array
     */
    protected void setSensitiveWords(Set<String> words) {
        if (!NATIVE_AVAILABLE) {
            log.warn("Sensitive words native detector is unavailable, skipping initialization.");
            return;
        }
        if (words == null || words.size() == 0) {
            log.warn("No sensitive words provided.");
            return;
        }
        try {
            // Create a temporary file to store sensitive words
            Path tempFile = Files.createTempFile("sensitive_words", ".txt");
            Files.write(tempFile, words);
            // Initialize sensitive words from the temporary file
            setSensitiveWordsFromFile(tempFile.toAbsolutePath().toString());
            // Delete the temporary file on exit
            tempFile.toFile().deleteOnExit();
        } catch (Exception e) {
            log.error("Failed to set sensitive words.", e);
        }
    }

    /**
     * Set sensitive words from file.
     *
     * @param filePath the file path of sensitive words, separated by new lines
     */
    protected void setSensitiveWordsFromFile(String filePath) {
        if (!NATIVE_AVAILABLE) {
            log.warn("Sensitive words native detector is unavailable, skipping initialization.");
            return;
        }
        // check file existence
        final Path path = Path.of(filePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.error("Sensitive words file does not exist: {}", filePath);
            return;
        }
        try {
            initSensitiveWords(filePath);
        } catch (RuntimeException e) {
            log.error("Failed to initialize sensitive words from file: {}", filePath, e);
        }
    }

    /**
     * Initialize sensitive words from file.
     * <p>
     * Supports being called once at application start; no additional locking is
     * applied to the Java side for performance.
     * </p>
     *
     * @param filePath the file path of sensitive words, separated by new lines
     */
    private native void initSensitiveWords(String filePath);

    /**
     * Check whether the content contains sensitive words
     * 
     * @param content the content to be checked
     * @return true if contains sensitive words, false otherwise
     */
    private native boolean checkSensitiveWordsBytes(byte[] content);

    /**
     * Check whether the content contains sensitive words
     * 
     * @param content the content to be checked
     * @return true if contains sensitive words, false otherwise
     * @implNote Whitespace characters (space, tab, newline, etc.) are ignored
     *           during native matching.
     */
    public boolean containsSensitiveWords(String content) {
        if (!NATIVE_AVAILABLE) {
            return false;
        }
        if (content == null || content.isBlank()) {
            return false;
        }
        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            return checkSensitiveWordsBytes(contentBytes);
        } catch (RuntimeException e) {
            log.error("Failed to check sensitive words.", e);
            return false;
        }
    }

}
