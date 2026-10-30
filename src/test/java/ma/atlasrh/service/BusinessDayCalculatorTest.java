package ma.atlasrh.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessDayCalculatorTest {

    @Test
    @DisplayName("Semaine complète du lundi au vendredi = 5 jours ouvrés")
    void fullWeek_shouldReturnFiveDays() {
        // Lundi 2026-11-02 au Vendredi 2026-11-06
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate end = LocalDate.of(2026, 11, 6);

        int days = BusinessDayCalculator.calculateBusinessDays(start, end);
        assertThat(days).isEqualTo(5);
    }

    @Test
    @DisplayName("Période chevauchant le week-end : Vendredi au Lundi = 2 jours ouvrés")
    void weekendOverlap_shouldExcludeSaturdayAndSunday() {
        // Vendredi 2026-11-06 au Lundi 2026-11-09
        LocalDate start = LocalDate.of(2026, 11, 6);
        LocalDate end = LocalDate.of(2026, 11, 9);

        int days = BusinessDayCalculator.calculateBusinessDays(start, end);
        assertThat(days).isEqualTo(2); // Vendredi + Lundi
    }

    @Test
    @DisplayName("Période uniquement le week-end (Samedi à Dimanche) = 0 jour ouvré")
    void onlyWeekend_shouldReturnZero() {
        LocalDate start = LocalDate.of(2026, 11, 7); // Samedi
        LocalDate end = LocalDate.of(2026, 11, 8);   // Dimanche

        int days = BusinessDayCalculator.calculateBusinessDays(start, end);
        assertThat(days).isEqualTo(0);
    }

    @Test
    @DisplayName("Une seule journée (Lundi) = 1 jour ouvré")
    void singleBusinessDay_shouldReturnOne() {
        LocalDate date = LocalDate.of(2026, 11, 2); // Lundi
        int days = BusinessDayCalculator.calculateBusinessDays(date, date);
        assertThat(days).isEqualTo(1);
    }

    @Test
    @DisplayName("Date de fin avant date de début doit lever IllegalArgumentException")
    void endDateBeforeStartDate_shouldThrow() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate end = LocalDate.of(2026, 11, 5);

        assertThatThrownBy(() -> BusinessDayCalculator.calculateBusinessDays(start, end))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Répartition des jours ouvrés à cheval sur deux années : 29/12/2025 au 05/01/2026")
    void calculateBusinessDaysForYear_spanningTwoYears_shouldSplitCorrectly() {
        // Lundi 29/12/2025 au Lundi 05/01/2026 (6 jours ouvrés au total : 3 en 2025, 3 en 2026)
        LocalDate start = LocalDate.of(2025, 12, 29);
        LocalDate end = LocalDate.of(2026, 1, 5);

        int days2025 = BusinessDayCalculator.calculateBusinessDaysForYear(start, end, 2025);
        int days2026 = BusinessDayCalculator.calculateBusinessDaysForYear(start, end, 2026);
        int days2027 = BusinessDayCalculator.calculateBusinessDaysForYear(start, end, 2027);

        assertThat(days2025).isEqualTo(3); // 29 (Lun), 30 (Mar), 31 (Mer)
        assertThat(days2026).isEqualTo(3); // 01 (Jeu), 02 (Ven), 05 (Lun)
        assertThat(days2027).isEqualTo(0);
        assertThat(days2025 + days2026).isEqualTo(BusinessDayCalculator.calculateBusinessDays(start, end));
    }
}
