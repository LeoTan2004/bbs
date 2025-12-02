#define _POSIX_C_SOURCE 200809L

#include <jni.h>
#include <ctype.h>
#include <errno.h>
#include <limits.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#define TRANSITIONS 256

typedef struct TrieNode {
    int transitions[TRANSITIONS];
    int fail;
    int output;
} TrieNode;

static TrieNode *g_nodes = NULL;
static size_t g_node_count = 0;
static size_t g_node_capacity = 0;
static atomic_bool g_ready = ATOMIC_VAR_INIT(false);
static pthread_rwlock_t g_lock;
static atomic_bool g_lock_initialized = ATOMIC_VAR_INIT(false);

static void throw_java_exception(JNIEnv *env, const char *class_name, const char *message);
static void reset_automaton(void);
static int create_node(void);
static int insert_word(const unsigned char *word, size_t length);
static int build_failure_links(void);
static size_t trim_word(char **start, char *end);
static int safe_state(int state);
static int safe_transition(int state, unsigned char c);
static bool is_zero_width_sequence(const unsigned char *text, size_t length, size_t index, size_t *consumed);

static void throw_java_exception(JNIEnv *env, const char *class_name, const char *message) {
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    jclass clazz = (*env)->FindClass(env, class_name);
    if (clazz == NULL) {
        (*env)->ExceptionClear(env);
        clazz = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (clazz == NULL) {
            return;
        }
    }
    (*env)->ThrowNew(env, clazz, message);
}

static void reset_automaton(void) {
    free(g_nodes);
    g_nodes = NULL;
    g_node_count = 0;
    g_node_capacity = 0;
    atomic_store_explicit(&g_ready, false, memory_order_release);
}

static int ensure_capacity(void) {
    if (g_node_count < g_node_capacity) {
        return 0;
    }

    size_t new_capacity = (g_node_capacity == 0) ? 256 : (g_node_capacity * 2);
    TrieNode *new_nodes = realloc(g_nodes, new_capacity * sizeof(TrieNode));
    if (new_nodes == NULL) {
        return -1;
    }

    g_nodes = new_nodes;
    g_node_capacity = new_capacity;
    return 0;
}

static int create_node(void) {
    if (g_node_count >= (size_t)INT_MAX) {
        return -1;
    }
    if (ensure_capacity() != 0) {
        return -1;
    }

    TrieNode *node = &g_nodes[g_node_count];
    for (int i = 0; i < TRANSITIONS; ++i) {
        node->transitions[i] = -1;
    }
    node->fail = 0;
    node->output = 0;

    int index = (int)g_node_count;
    g_node_count++;
    return index;
}

static size_t trim_word(char **start, char *end) {
    char *s = *start;
    while (s < end && isspace((unsigned char)*s)) {
        s++;
    }
    while (end > s && isspace((unsigned char)*(end - 1))) {
        end--;
    }
    *start = s;
    size_t length = (size_t)(end - s);
    if (length > 0) {
        s[length] = '\0';
    }
    return length;
}

static int safe_state(int state) {
    if (g_node_count == 0) {
        return 0;
    }
    if (state < 0 || (size_t)state >= g_node_count) {
        return 0;
    }
    return state;
}

static int safe_transition(int state, unsigned char c) {
    if (g_node_count == 0) {
        return 0;
    }
    int normalized = safe_state(state);
    int next = g_nodes[normalized].transitions[c];
    if (next < 0) {
        return 0;
    }
    if ((size_t)next >= g_node_count) {
        return 0;
    }
    return next;
}

static bool is_zero_width_sequence(const unsigned char *text, size_t length, size_t index, size_t *consumed) {
    if (text == NULL || consumed == NULL || index >= length) {
        return false;
    }

    size_t remaining = length - index;

    if (remaining >= 3 && text[index] == 0xE2 && text[index + 1] == 0x80) {
        unsigned char third = text[index + 2];
        if (third == 0x8B || third == 0x8C || third == 0x8D || third == 0x8E) {
            *consumed = 3;
            return true;
        }
    }

    if (remaining >= 3 && text[index] == 0xE2 && text[index + 1] == 0x81 && text[index + 2] == 0xA0) {
        *consumed = 3; // U+2060 WORD JOINER
        return true;
    }

    if (remaining >= 3 && text[index] == 0xEF && text[index + 1] == 0xBB && text[index + 2] == 0xBF) {
        *consumed = 3; // U+FEFF ZERO WIDTH NO-BREAK SPACE
        return true;
    }

    return false;
}

static int insert_word(const unsigned char *word, size_t length) {
    if (length == 0) {
        return 0;
    }
    if (g_node_count == 0) {
        return -1;
    }

    int state = safe_state(0);
    for (size_t i = 0; i < length; ++i) {
        unsigned char c = word[i];
        int next = (g_node_count > 0) ? g_nodes[state].transitions[c] : -1;
        if (next == -1) {
            int new_state = create_node();
            if (new_state < 0) {
                return -1;
            }
            g_nodes[state].transitions[c] = new_state;
            next = new_state;
        } else if ((size_t)next >= g_node_count) {
            return -1;
        }
        state = next;
    }
    if (g_node_count > 0 && (size_t)state < g_node_count) {
        g_nodes[state].output = 1;
    }
    return 0;
}

static int build_failure_links(void) {
    if (g_node_count == 0) {
        return 0;
    }

    int *queue = malloc(g_node_count * sizeof(int));
    if (queue == NULL) {
        return -1;
    }

    size_t head = 0;
    size_t tail = 0;

    for (int c = 0; c < TRANSITIONS; ++c) {
        int next = g_nodes[0].transitions[c];
        if (next != -1) {
            if ((size_t)next >= g_node_count) {
                free(queue);
                return -1;
            }
            g_nodes[next].fail = 0;
            if (tail >= g_node_count) {
                free(queue);
                return -1;
            }
            queue[tail++] = next;
        } else {
            g_nodes[0].transitions[c] = 0;
        }
    }

    while (head < tail) {
        int state = queue[head++];
        if ((size_t)state >= g_node_count) {
            free(queue);
            return -1;
        }
        for (int c = 0; c < TRANSITIONS; ++c) {
            int next = g_nodes[state].transitions[c];
            if (next != -1) {
                if ((size_t)next >= g_node_count) {
                    free(queue);
                    return -1;
                }
                int fail_state = g_nodes[state].fail;
                if (fail_state < 0 || (size_t)fail_state >= g_node_count) {
                    fail_state = 0;
                }
                int fallback = g_nodes[fail_state].transitions[c];
                if (fallback < 0 || (size_t)fallback >= g_node_count) {
                    fallback = 0;
                }
                g_nodes[next].fail = fallback;
                if (g_nodes[fallback].output) {
                    g_nodes[next].output = 1;
                }
                if (tail >= g_node_count) {
                    free(queue);
                    return -1;
                }
                queue[tail++] = next;
            } else {
                int fail_state = g_nodes[state].fail;
                if (fail_state < 0 || (size_t)fail_state >= g_node_count) {
                    fail_state = 0;
                }
                g_nodes[state].transitions[c] = g_nodes[fail_state].transitions[c];
            }
        }
    }

    free(queue);
    return 0;
}

JNIEXPORT void JNICALL Java_edu_xtu_bbs_common_validation_SensitiveWordsDetector_initSensitiveWords(JNIEnv *env, jobject obj, jstring filePath) {
    (void)obj; // unused

    if (filePath == NULL) {
        throw_java_exception(env, "java/lang/IllegalArgumentException", "File path cannot be null");
        return;
    }

    const char *native_path = (*env)->GetStringUTFChars(env, filePath, NULL);
    if (native_path == NULL) {
        return; // JVM threw an exception
    }

    FILE *fp = NULL;
    char *line = NULL;
    size_t line_capacity = 0;
    ssize_t line_length = 0;
    size_t words_inserted = 0;
    bool lock_acquired = false;
    bool automaton_built = false;
    const char *exception_class = NULL;
    char message[512] = {0};

    if (!atomic_load_explicit(&g_lock_initialized, memory_order_acquire)) {
        exception_class = "java/lang/IllegalStateException";
        snprintf(message, sizeof(message), "Native detector not initialized");
        goto cleanup;
    }

    if (pthread_rwlock_wrlock(&g_lock) != 0) {
        exception_class = "java/lang/IllegalStateException";
        snprintf(message, sizeof(message), "Failed to acquire write lock");
        goto cleanup;
    }
    lock_acquired = true;

    reset_automaton();

    if (create_node() < 0) {
        exception_class = "java/lang/OutOfMemoryError";
        snprintf(message, sizeof(message), "Failed to allocate root node for automaton");
        goto cleanup;
    }

    fp = fopen(native_path, "r");
    if (fp == NULL) {
        exception_class = "java/io/IOException";
        snprintf(message, sizeof(message), "Unable to open sensitive words file: %s", strerror(errno));
        goto cleanup;
    }

    errno = 0;
    while ((line_length = getline(&line, &line_capacity, fp)) != -1) {
        char *start = line;
        char *end = line + line_length;
        size_t trimmed_length = trim_word(&start, end);
        if (trimmed_length == 0) {
            continue;
        }

        if (insert_word((const unsigned char *)start, trimmed_length) != 0) {
            exception_class = "java/lang/OutOfMemoryError";
            snprintf(message, sizeof(message), "Failed to insert word into automaton");
            goto cleanup;
        }

        words_inserted++;
    }

    if (ferror(fp)) {
        exception_class = "java/io/IOException";
        snprintf(message, sizeof(message), "Failed to read sensitive words file: %s", strerror(errno));
        goto cleanup;
    }

    if (words_inserted == 0) {
        goto cleanup;
    }

    if (build_failure_links() != 0) {
        exception_class = "java/lang/OutOfMemoryError";
        snprintf(message, sizeof(message), "Failed to build automaton failure links");
        goto cleanup;
    }

    atomic_store_explicit(&g_ready, true, memory_order_release);
    automaton_built = true;

cleanup:
    if (line != NULL) {
        free(line);
    }
    if (fp != NULL) {
        fclose(fp);
    }
    if (lock_acquired) {
        if (!automaton_built) {
            reset_automaton();
        }
        pthread_rwlock_unlock(&g_lock);
    }
    (*env)->ReleaseStringUTFChars(env, filePath, native_path);
    if (exception_class != NULL) {
        throw_java_exception(env, exception_class, message);
    }
}

JNIEXPORT jboolean JNICALL Java_edu_xtu_bbs_common_validation_SensitiveWordsDetector_checkSensitiveWordsBytes(JNIEnv *env, jobject obj, jbyteArray content) {
    (void)obj; // unused

    if (content == NULL) {
        return JNI_FALSE;
    }

    const char *exception_class = NULL;
    char message[128] = {0};
    bool lock_acquired = false;
    jboolean result = JNI_FALSE;

    jbyte *buffer = NULL;
    jsize length = (*env)->GetArrayLength(env, content);
    if (length < 0) {
        return JNI_FALSE;
    }

    buffer = (*env)->GetByteArrayElements(env, content, NULL);
    if (buffer == NULL) {
        return JNI_FALSE; // JVM threw an exception
    }

    if (!atomic_load_explicit(&g_lock_initialized, memory_order_acquire)) {
        exception_class = "java/lang/IllegalStateException";
        snprintf(message, sizeof(message), "Native detector not initialized");
        goto cleanup;
    }

    if (pthread_rwlock_rdlock(&g_lock) != 0) {
        exception_class = "java/lang/IllegalStateException";
        snprintf(message, sizeof(message), "Failed to acquire read lock");
        goto cleanup;
    }
    lock_acquired = true;

    bool ready = atomic_load_explicit(&g_ready, memory_order_acquire);
    if (!ready || g_nodes == NULL || g_node_count == 0) {
        goto cleanup;
    }

    const unsigned char *text = (const unsigned char *)buffer;
    int state = safe_state(0);

    for (jsize i = 0; i < length; ++i) {
        unsigned char c = text[i];

        if (isspace(c)) {
            continue;
        }

        size_t consumed = 0;
        if (is_zero_width_sequence(text, (size_t)length, (size_t)i, &consumed)) {
            i += (jsize)(consumed - 1);
            continue;
        }

        state = safe_transition(state, c);
        if ((size_t)state < g_node_count && g_nodes[state].output) {
            result = JNI_TRUE;
            break;
        }
    }

cleanup:
    if (lock_acquired) {
        pthread_rwlock_unlock(&g_lock);
    }
    if (buffer != NULL) {
        (*env)->ReleaseByteArrayElements(env, content, buffer, JNI_ABORT);
    }
    if (exception_class != NULL) {
        throw_java_exception(env, exception_class, message);
    }
    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;

    if (atomic_load_explicit(&g_lock_initialized, memory_order_acquire)) {
        return JNI_VERSION_1_8;
    }

    if (pthread_rwlock_init(&g_lock, NULL) != 0) {
        return JNI_ERR;
    }
    atomic_store_explicit(&g_lock_initialized, true, memory_order_release);
    reset_automaton();
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;

    if (!atomic_load_explicit(&g_lock_initialized, memory_order_acquire)) {
        return;
    }

    if (pthread_rwlock_wrlock(&g_lock) == 0) {
        reset_automaton();
        pthread_rwlock_unlock(&g_lock);
    }
    pthread_rwlock_destroy(&g_lock);
    atomic_store_explicit(&g_lock_initialized, false, memory_order_release);
}
