package edu.xtu.bbs.common.validation;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

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
            log.debug("Failed to load sensitive words native library via System.loadLibrary.", e);
            log.warn("Failed to load sensitive words native library via java.library.path.");
            try {
                Path devLibrary = Path.of("target", "native", System.mapLibraryName(LIB_NAME));
                if (Files.exists(devLibrary)) {
                    System.load(devLibrary.toAbsolutePath().toString());
                    loaded = true;
                    log.info("Loaded sensitive words native library from {}", devLibrary);
                }
            } catch (UnsatisfiedLinkError | SecurityException inner) {
                log.warn("Failed to load sensitive words native library from development path.");
            }
            if (loaded || loadLibraryFromJarClasspath()) {
                loaded = true;
            } else {
                log.error("Sensitive words native library is not available.");
            }
        }
        NATIVE_AVAILABLE = loaded;
    }

    private static boolean loadLibraryFromJarClasspath() {
        String libraryFileName = System.mapLibraryName(LIB_NAME);
        String resourcePath = "META-INF/native/" + libraryFileName;
        try (InputStream inputStream = SensitiveWordsDetector.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return loadLib(libraryFileName, resourcePath, inputStream);
        } catch (UnsatisfiedLinkError | SecurityException e) {
            log.debug("Failed to load sensitive words native library from classpath.", e);
            log.warn("Failed to load sensitive words native library extracted from classpath.");
            return false;
        } catch (IOException e) {
            log.debug("Failed to extract sensitive words native library from classpath.", e);
            log.warn("Failed to extract sensitive words native library from classpath.");
            return false;
        }
    }

    private static boolean loadLib(String libraryFileName, String resourcePath, InputStream inputStream) throws IOException {
        if (inputStream == null) {
            log.warn("Sensitive words native library not found on classpath: {}", resourcePath);
            return false;
        }
        Path tempDir = Files.createTempDirectory("sensitive_words_native_");
        Path tempLibrary = tempDir.resolve(libraryFileName);
        Files.copy(inputStream, tempLibrary, StandardCopyOption.REPLACE_EXISTING);
        System.load(tempLibrary.toAbsolutePath().toString());
        tempLibrary.toFile().deleteOnExit();
        tempDir.toFile().deleteOnExit();
        log.info("Loaded sensitive words native library from classpath resource {}", resourcePath);
        return true;
    }

    /**
     * Set sensitive words.
     *
     * @param words the sensitive words array
     * @apiNote Words are persisted to a temporary UTF-8 encoded file before the
     * native matcher loads them, so inputs must be encoded consistently
     * with the application text.
     */
    protected void setSensitiveWords(Set<String> words) {
        if (!NATIVE_AVAILABLE) {
            log.warn("Sensitive words native detector is unavailable, skipping initialization.");
            return;
        }
        if (words == null || words.isEmpty()) {
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
     * during native matching.
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
