package no.nav.pto.veilarbportefolje.postgres.opensearch.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetType;
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

import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetType.mote;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnDatoerEtterDagensDato;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnForrigeAktivitetStartDatoer;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnNyesteUtlopteAktivAktivitet;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Data
@Accessors(chain = true)
public class AktivitetSamling {
    List<AktivitetEntity> avtalteAktiveAktivteter = new ArrayList<>();
    HashSet<String> tiltak = new HashSet<>();

    public PostgresAktivitetEntity bygg() {
        PostgresAktivitetEntity entity = new PostgresAktivitetEntity();

        byggAktivitetBrukerData(entity, avtalteAktiveAktivteter);
        byggAktivitetStatusBrukerData(entity, avtalteAktiveAktivteter);

        return entity.setAktiviteter(avtalteAktiveAktivteter.stream()
                        .map(AktivitetEntity::getAktivitetType)
                        .map(AktivitetType::name)
                        .collect(Collectors.toSet()))
                .setTiltak(tiltak);
    }

    private void byggAktivitetStatusBrukerData(PostgresAktivitetEntity postgresAktivitetEntity, List<AktivitetEntity> alleAktiviter) {
        LocalDate idag = LocalDate.now();

        Timestamp moteFremtidigUtlopsdato = null;
        Timestamp moteFremtidigStartdato = null;
        Timestamp stillingFremtidigUtlopsdato = null;
        Timestamp egenFremtidigUtlopsdato = null;
        Timestamp behandlingFremtidigUtlopsdato = null;
        Timestamp ijobbFremtidigUtlopsdato = null;
        Timestamp sokeavtaleFremtidigUtlopsdato = null;
        Timestamp tiltakFremtidigUtlopsdato = null;
        Timestamp utdanningaktivitetFremtidigUtlopsdato = null;
        Timestamp gruppeaktivitetFremtidigUtlopsdato = null;

        for (AktivitetEntity aktivitet : alleAktiviter) {
            if (aktivitet.aktivitetType.equals(mote) && !(aktivitet.getStart() == null || aktivitet.getUtlop().toLocalDateTime().toLocalDate().isBefore(idag))) {
                moteFremtidigStartdato = nesteFremITiden(moteFremtidigStartdato, aktivitet.getStart());
            }
            if (aktivitet.getUtlop() == null || idag.isAfter(aktivitet.getUtlop().toLocalDateTime().toLocalDate())) {
                continue;
            }
            switch (aktivitet.aktivitetType) {
                case egen -> egenFremtidigUtlopsdato = nesteFremITiden(egenFremtidigUtlopsdato, aktivitet.getUtlop());
                case stilling -> stillingFremtidigUtlopsdato = nesteFremITiden(stillingFremtidigUtlopsdato, aktivitet.getUtlop());
                case sokeavtale -> sokeavtaleFremtidigUtlopsdato = nesteFremITiden(sokeavtaleFremtidigUtlopsdato, aktivitet.getUtlop());
                case behandling -> behandlingFremtidigUtlopsdato = nesteFremITiden(behandlingFremtidigUtlopsdato, aktivitet.getUtlop());
                case ijobb -> ijobbFremtidigUtlopsdato = nesteFremITiden(ijobbFremtidigUtlopsdato, aktivitet.getUtlop());
                case tiltak -> tiltakFremtidigUtlopsdato = nesteFremITiden(tiltakFremtidigUtlopsdato, aktivitet.getUtlop());
                case gruppeaktivitet -> gruppeaktivitetFremtidigUtlopsdato = nesteFremITiden(gruppeaktivitetFremtidigUtlopsdato, aktivitet.getUtlop());
                case utdanningaktivitet -> utdanningaktivitetFremtidigUtlopsdato = nesteFremITiden(utdanningaktivitetFremtidigUtlopsdato, aktivitet.getUtlop());
                case mote -> moteFremtidigUtlopsdato = nesteFremITiden(moteFremtidigUtlopsdato, aktivitet.getUtlop());
            }
        }
        postgresAktivitetEntity
                .setAktivitetEgenUtlopsdato(toIsoUTC(egenFremtidigUtlopsdato))
                .setAktivitetStillingUtlopsdato(toIsoUTC(stillingFremtidigUtlopsdato))
                .setAktivitetMoteStartdato(toIsoUTC(moteFremtidigStartdato))
                .setAktivitetMoteUtlopsdato(toIsoUTC(moteFremtidigUtlopsdato))
                .setAktivitetBehandlingUtlopsdato(toIsoUTC(behandlingFremtidigUtlopsdato))
                .setAktivitetIjobbUtlopsdato(toIsoUTC(ijobbFremtidigUtlopsdato))
                .setAktivitetSokeavtaleUtlopsdato(toIsoUTC(sokeavtaleFremtidigUtlopsdato))
                .setAktivitetTiltakUtlopsdato(toIsoUTC(tiltakFremtidigUtlopsdato))
                .setAktivitetUtdanningaktivitetUtlopsdato(toIsoUTC(utdanningaktivitetFremtidigUtlopsdato))
                .setAktivitetGruppeaktivitetUtlopsdato(toIsoUTC(gruppeaktivitetFremtidigUtlopsdato));

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

    private Timestamp nesteFremITiden(Timestamp a, Timestamp b) {
        return a == null ? b : (b == null ? a : (a.before(b) ? a : b));
    }
}
