package me.josecomparotto.contabilidade_pessoal.application.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;

@Converter(autoApply = false)
public class NaturezaConverter implements AttributeConverter<Natureza, Boolean> {

    @Override
    public Boolean convertToDatabaseColumn(Natureza attribute) {
        if (attribute == null) return null;
        return attribute == Natureza.CREDORA;
    }

    @Override
    public Natureza convertToEntityAttribute(Boolean dbData) {
        if (dbData == null) return null;
        return dbData ? Natureza.CREDORA : Natureza.DEVEDORA;
    }
}
