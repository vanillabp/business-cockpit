package io.vanillabp.cockpit.commons.mongo.converters;

import com.mongodb.lang.NonNull;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.math.BigDecimal;

@WritingConverter
public class BigDecimalReadConverter implements Converter<BigDecimal, Decimal128> {

    @Override
    public Decimal128 convert(@NonNull BigDecimal source) {
        return new Decimal128(source);
    }

}
