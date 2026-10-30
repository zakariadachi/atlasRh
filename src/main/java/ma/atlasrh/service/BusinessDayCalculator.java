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
}
