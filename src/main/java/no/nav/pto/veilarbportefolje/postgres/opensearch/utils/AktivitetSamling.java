package no.nav.pto.veilarbportefolje.postgres.opensearch.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.postgres.opensearch.PostgresAktivitetEntity;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnDatoerEtterDagensDato;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnForrigeAktivitetStartDatoer;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnNyesteUtlopteAktivAktivitet;

@Data
@Accessors(chain = true)
public class AktivitetSamling {
    List<AktivitetEntity> avtalteAktiveAktivteter = new ArrayList<>();
    HashSet<String> tiltak = new HashSet<>();

    public PostgresAktivitetEntity bygg() {
        PostgresAktivitetEntity entity = new PostgresAktivitetEntity();

        byggAktivitetBrukerData(entity, avtalteAktiveAktivteter);
        byggAktivitetStatusBrukerData(entity, avtalteAktiveAktivteter);

        return entity
                .setAktiviteter(avtalteAktiveAktivteter.stream().map(AktivitetEntity::getAktivitetType).collect(Collectors.toSet()))
                .setTiltak(tiltak);
    }

    private void byggAktivitetStatusBrukerData(PostgresAktivitetEntity entity, List<AktivitetEntity> alleAktiviter) {
    }

    private void byggAktivitetBrukerData(PostgresAktivitetEntity postgresAktivitetEntity, List<AktivitetEntity> alleAktiviter) {
        LocalDate idag = LocalDate.now();

        List<Timestamp> startDatoer = alleAktiviter.stream().map(AktivitetEntity::getStart).filter(Objects::nonNull).toList();
        List<Timestamp> sluttdatoer = alleAktiviter.stream().map(AktivitetEntity::getUtlop).filter(Objects::nonNull).toList();

        Optional<Timestamp> nyesteUtlopteDato = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(sluttdatoer, idag));
        Optional<Timestamp> forrigeAktivitetStart = Optional.ofNullable(finnForrigeAktivitetStartDatoer(startDatoer, idag));

        List<Timestamp> startDatoerEtterDagensDato = finnDatoerEtterDagensDato(startDatoer, idag);
        Optional<Timestamp> aktivitetStart = (startDatoerEtterDagensDato.isEmpty()) ? Optional.empty() : Optional.ofNullable(startDatoerEtterDagensDato.get(0));
        Optional<Timestamp> nesteAktivitetStart = (startDatoerEtterDagensDato.size() < 2) ? Optional.empty() : Optional.ofNullable(startDatoerEtterDagensDato.get(1));

        postgresAktivitetEntity.setAktivitetStart(aktivitetStart.map(DateUtils::toIsoUTC).orElse(null))
                .setNesteAktivitetStart(nesteAktivitetStart.map(DateUtils::toIsoUTC).orElse(null))
                .setNyesteUtlopteAktivitet(nyesteUtlopteDato.map(DateUtils::toIsoUTC).orElse(null))
                .setForrigeAktivitetStart(forrigeAktivitetStart.map(DateUtils::toIsoUTC).orElse(null));
    }
}
