package dev.einselbst.ghostdash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCalculatorTest {
    @Test
    void scalesWithActualRouteDistance() {
        assertEquals(10.5, DamageCalculator.calculate(10.0, 4.0, 0.65, 20.0));
    }

    @Test
    void neverExceedsConfiguredCap() {
        assertEquals(20.0, DamageCalculator.calculate(64.0, 4.0, 0.65, 20.0));
    }

    @Test
    void clampsNegativeInputs() {
        assertEquals(0.0, DamageCalculator.calculate(-2.0, -4.0, -1.0, -20.0));
    }
}
