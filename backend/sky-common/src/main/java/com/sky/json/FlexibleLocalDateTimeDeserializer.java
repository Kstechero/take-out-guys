package com.sky.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Supports both API-standard ISO date-time and the project's legacy space-separated format. */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter SPACE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SPACE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();
        if (value == null || value.trim().isEmpty()) return null;
        value = value.trim();
        try {
            if (value.indexOf('T') >= 0) return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return LocalDateTime.parse(value, value.length() > 16 ? SPACE_SECOND : SPACE_MINUTE);
        } catch (DateTimeParseException ex) {
            throw context.weirdStringException(value, LocalDateTime.class,
                    "日期时间格式应为 yyyy-MM-dd HH:mm[:ss] 或 yyyy-MM-dd'T'HH:mm[:ss]");
        }
    }
}
