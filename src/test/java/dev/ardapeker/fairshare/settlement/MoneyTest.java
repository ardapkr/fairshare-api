package dev.ardapeker.fairshare.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"12.50, 1250", "12.5, 1250", "12, 1200", "0.01, 1", "12.500, 1250"})
    void convertsDecimalsToCents(String amount, long cents) {
        assertThat(Money.toCents(new BigDecimal(amount))).isEqualTo(cents);
    }

    @Test
    void rejectsFractionsOfACent() {
        assertThatThrownBy(() -> Money.toCents(new BigDecimal("1.005")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatsCentsWithTwoDecimals() {
        assertThat(Money.fromCents(1250)).isEqualByComparingTo("12.50");
        assertThat(Money.fromCents(1250).toPlainString()).isEqualTo("12.50");
        assertThat(Money.fromCents(-5).toPlainString()).isEqualTo("-0.05");
    }
}
