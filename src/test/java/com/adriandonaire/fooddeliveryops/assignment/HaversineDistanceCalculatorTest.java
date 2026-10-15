package com.adriandonaire.fooddeliveryops.assignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineDistanceCalculatorTest {

    @Test
    @DisplayName("Distancia entre el mismo punto GPS debe ser 0.0")
    void samePointShouldBeZeroDistance() {
        double distance = HaversineDistanceCalculator.calculateDistanceKm(-34.6177, -68.3301, -34.6177, -68.3301);
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Distancia calculada entre dos puntos conocidos debe ser precisa")
    void calculatedDistanceBetweenKnownPoints() {
        // San Rafael center (-34.6177, -68.3301) to nearby point approx 1.1 km away (-34.6080, -68.3301)
        double distance = HaversineDistanceCalculator.calculateDistanceKm(-34.6177, -68.3301, -34.6080, -68.3301);
        assertThat(distance).isGreaterThan(1.0).isLessThan(1.2);
    }
}
