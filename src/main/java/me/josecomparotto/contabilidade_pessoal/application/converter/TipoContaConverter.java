package me.josecomparotto.contabilidade_pessoal.application.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

@Converter(autoApply = false)
public class TipoContaConverter implements AttributeConverter<TipoConta, Boolean> {

    @Override
    public Boolean convertToDatabaseColumn(TipoConta attribute) {
        if (attribute == null) return null;
        return attribute == TipoConta.ANALITICA;
    }

    @Override
    public TipoConta convertToEntityAttribute(Boolean dbData) {
        if (dbData == null) return null;
        return dbData ? TipoConta.ANALITICA : TipoConta.SINTETICA;
    }
}
