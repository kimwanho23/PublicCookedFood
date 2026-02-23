package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SoftDeleteStateConverter implements AttributeConverter<SoftDeleteState, String> {

    @Override
    public String convertToDatabaseColumn(SoftDeleteState attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public SoftDeleteState convertToEntityAttribute(String dbData) {
        return SoftDeleteState.fromDbValue(dbData);
    }
}
