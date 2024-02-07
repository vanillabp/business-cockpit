package io.vanillabp.cockpit.commons.mongo.converters;

import com.mongodb.lang.NonNull;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.math.BigDecimal;

@ReadingConverter
public class BigDecimalWriteConverter implements Converter<Decimal128, BigDecimal> {

    @Override
    public BigDecimal convert(@NonNull Decimal128 source) {
        return source.bigDecimalValue();
    }

}
