package edu.xtu.bbs.post.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MediumListConverter unit tests")
class MediumListConverterTest {

    private final MediumListConverter converter = new MediumListConverter();

    @Test
    @DisplayName("convertToDatabaseColumn: null attribute returns null")
    void convertToDatabaseColumn_NullAttribute_ReturnsNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result, "Expected null when converting null attribute to DB column");
    }

    @Test
    @DisplayName("convertToDatabaseColumn: empty list returns JSON empty array")
    void convertToDatabaseColumn_EmptyList_ReturnsEmptyJsonArray() {
        String json = converter.convertToDatabaseColumn(Collections.emptyList());
        assertNotNull(json, "JSON string should not be null for empty list");
        assertEquals("[]", json, "Empty list should be serialized to []");
    }

    @Test
    @DisplayName("convertToEntityAttribute: null or blank DB data returns empty list")
    void convertToEntityAttribute_NullOrBlank_ReturnsEmptyList() {
        List<?> fromNull = converter.convertToEntityAttribute(null);
        List<?> fromEmpty = converter.convertToEntityAttribute("");
        List<?> fromBlank = converter.convertToEntityAttribute("   ");

        assertNotNull(fromNull, "Should return a non-null list even for null DB data");
        assertTrue(fromNull.isEmpty(), "Expected empty list for null DB data");

        assertNotNull(fromEmpty, "Should return a non-null list for empty string");
        assertTrue(fromEmpty.isEmpty(), "Expected empty list for empty string DB data");

        assertNotNull(fromBlank, "Should return a non-null list for blank string");
        assertTrue(fromBlank.isEmpty(), "Expected empty list for blank string DB data");
    }

    @Test
    @DisplayName("convertToEntityAttribute: empty JSON array returns empty list")
    void convertToEntityAttribute_EmptyJsonArray_ReturnsEmptyList() {
        List<?> result = converter.convertToEntityAttribute("[]");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "[] should deserialize to an empty list");
    }

    @Test
    @DisplayName("Round-trip: serialize then deserialize preserves list size and elements exist")
    void roundTrip_SerializeDeserialize_PreservesListStructure() {
        // Create a Medium instance by deserializing a minimal JSON array with an empty object.
        // This avoids depending on Medium constructors or setters.
        List<?> parsed = converter.convertToEntityAttribute("[{}]");
        assertNotNull(parsed, "Parsed list from \"[{}]\" should not be null");
        assertEquals(1, parsed.size(), "Parsed list should contain one element");
        assertNotNull(parsed.get(0), "Element parsed from {} should not be null");

        // Serialize the parsed list back to JSON
        String json = converter.convertToDatabaseColumn((List) parsed);
        assertNotNull(json, "Serialized JSON should not be null");
        assertTrue(json.startsWith("["), "Serialized JSON should be an array");
        assertTrue(json.endsWith("]"), "Serialized JSON should be an array");

        // Deserialize again and verify structure
        List<?> reparsed = converter.convertToEntityAttribute(json);
        assertNotNull(reparsed);
        assertEquals(1, reparsed.size(), "Re-parsed list should contain one element");
        assertNotNull(reparsed.get(0), "Element in re-parsed list should not be null");
    }

    @Test
    @DisplayName("convertToEntityAttribute: invalid JSON throws IllegalStateException")
    void convertToEntityAttribute_InvalidJson_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute("not-a-json"));
    }
}