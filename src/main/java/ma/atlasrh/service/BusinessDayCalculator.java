package ma.atlasrh.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Calculateur de jours ouvrés imposé par le brief :
 * Lundi à vendredi, dates comprises.
 */
public final class BusinessDayCalculator {

    private BusinessDayCalculator() {
    }

    /**
     * Calcule le nombre de jours ouvrés entre startDate et endDate (inclus).
     * Compte uniquement du lundi au vendredi.
     */
    public static int calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être nulles");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début");
        }

        int businessDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        return businessDays;
    }

    /**
     * Calcule le nombre de jours ouvrés d'une période [startDate, endDate] qui tombent dans une année donnée.
     * Compte uniquement du lundi au vendredi.
     */
    public static int calculateBusinessDaysForYear(LocalDate startDate, LocalDate endDate, int year) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être nulles");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début");
        }

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        if (endDate.isBefore(yearStart) || startDate.isAfter(yearEnd)) {
            return 0;
        }

        LocalDate effectiveStart = startDate.isBefore(yearStart) ? yearStart : startDate;
        LocalDate effectiveEnd = endDate.isAfter(yearEnd) ? yearEnd : endDate;

        return calculateBusinessDays(effectiveStart, effectiveEnd);
    }
}
