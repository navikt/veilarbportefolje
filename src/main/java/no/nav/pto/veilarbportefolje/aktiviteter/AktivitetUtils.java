package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AktivitetUtils {
    public static Timestamp finnNyesteUtlopteAktivAktivitet(List<Timestamp> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static Timestamp finnForrigeAktivitetStartDatoer(List<Timestamp> startDatoer, LocalDate today) {
        return startDatoer
                .stream()
                .filter(aktivitet -> aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static List<Timestamp> finnDatoerEtterDagensDato(List<Timestamp> aktiviteter, LocalDate today) {
        return aktiviteter
                .stream()
                .filter(aktivitet -> !aktivitet.toLocalDateTime().toLocalDate().isBefore(today))
                .sorted()
                .collect(Collectors.toList());
    }

    public static String statusToIsoUtcString(Timestamp utlopsdato) {
        return Optional.ofNullable(utlopsdato).map(DateUtils::toIsoUTC).orElse(DateUtils.getFarInTheFutureDate());
    }
}
