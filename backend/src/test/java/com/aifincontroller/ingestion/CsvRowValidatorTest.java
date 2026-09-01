package com.aifincontroller.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.aifincontroller.ingestion.dto.IngestionError;
import com.aifincontroller.ingestion.validation.CsvRowValidator;
import java.io.StringReader;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class CsvRowValidatorTest {

    private final CsvRowValidator validator = new CsvRowValidator();

    @Test
    void validPaymentRowProducesNoErrors() throws Exception {
        CSVRecord record = parse(
                "payment_id,order_id,amount,currency,status,created_at,captured_at\n"
                        + "pay_1,order_1,100.5000,INR,captured,2026-08-31T06:00:00Z,2026-08-31T06:01:00Z");

        List<IngestionError> errors = validator.validate("PAYMENT", record);

        assertThat(errors).isEmpty();
    }

    @Test
    void missingRequiredPaymentFieldProducesError() throws Exception {
        CSVRecord record = parse(
                "payment_id,order_id,amount,currency,status,created_at\n"
                        + "pay_1,,100.5000,INR,captured,2026-08-31T06:00:00Z");

        List<IngestionError> errors = validator.validate("PAYMENT", record);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("order_id");
            assertThat(error.getMessage()).isEqualTo("Required field is missing");
        });
    }

    @Test
    void invalidDecimalProducesError() throws Exception {
        CSVRecord record = parse(
                "payment_id,order_id,amount,currency,status,created_at\n"
                        + "pay_1,order_1,abc,INR,captured,2026-08-31T06:00:00Z");

        List<IngestionError> errors = validator.validate("PAYMENT", record);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("amount");
            assertThat(error.getMessage()).isEqualTo("Invalid decimal value");
        });
    }

    @Test
    void invalidTimestampProducesError() throws Exception {
        CSVRecord record = parse(
                "payment_id,order_id,amount,currency,status,created_at\n"
                        + "pay_1,order_1,100.5000,INR,captured,not-a-timestamp");

        List<IngestionError> errors = validator.validate("PAYMENT", record);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("created_at");
            assertThat(error.getMessage())
                    .isEqualTo("Invalid timestamp. Expected ISO-8601 format");
        });
    }

    @Test
    void unsupportedEntityTypeProducesError() throws Exception {
        CSVRecord record = parse(
                "payment_id\n"
                        + "pay_1");

        List<IngestionError> errors = validator.validate("UNKNOWN", record);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getField()).isNull();
        assertThat(errors.get(0).getMessage())
                .isEqualTo("Unsupported ingestion entity type: UNKNOWN");
    }

    private CSVRecord parse(String csv) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (CSVParser parser = format.parse(new StringReader(csv))) {
            return parser.getRecords().get(0);
        }
    }
}
