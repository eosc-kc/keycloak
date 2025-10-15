package org.keycloak.models.jpa.converter;

import java.io.IOException;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

public class ListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final Logger logger = Logger.getLogger(ListJsonConverter.class);

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {

        try {
            return JsonSerialization.writeValueAsString(attribute);
        } catch (IOException e) {
            logger.error("Error while converting List to JSON String");
            return null;
        }

    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {

        try {
            return JsonSerialization.readValue(dbData, List.class);
        } catch (IOException e) {
            logger.error("Error while converting JSON String to List ");
            return null;
        }

    }
}
