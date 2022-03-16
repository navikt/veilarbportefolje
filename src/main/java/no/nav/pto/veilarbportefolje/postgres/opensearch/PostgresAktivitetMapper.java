package no.nav.pto.veilarbportefolje.postgres.opensearch;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntityDto;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetStatusData;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.IkkeAvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType.mote;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnDatoerEtterDagensDato;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnForrigeAktivitetStartDatoer;
import static no.nav.pto.veilarbportefolje.database.BrukerDataService.finnNyesteUtlopteAktivAktivitet;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

public class PostgresAktivitetMapper {
    public static IkkeAvtaltAktivitetEntity kalkulerGenerellAktivitetInformasjon(List<AktivitetEntityDto> aktiviteter){
        IkkeAvtaltAktivitetEntity entity = new IkkeAvtaltAktivitetEntity();
        if(aktiviteter == null){
            return entity;
        }
        Set<String> aktiveAktiviteter = aktiviteter.stream()
                .map(AktivitetEntityDto::getAktivitetsType)
                .map(AktivitetsType::name)
                .collect(Collectors.toSet());
        byggAktivitetStatusBrukerData(entity, aktiviteter);

        return entity.setAlleAktiviteter(aktiveAktiviteter);
    }

    public static AvtaltAktivitetEntity kalkulerAvtalteAktivitetInformasjon(List<AktivitetEntityDto> avtalteAktivteter) {
       AvtaltAktivitetEntity entity = new AvtaltAktivitetEntity();
        if(avtalteAktivteter == null){
            return new AvtaltAktivitetEntity()
                    .setAktiviteter(new HashSet<>())
                    .setTiltak(new HashSet<>());
        }
        Set<String> aktiveAktiviteter = avtalteAktivteter.stream()
                .map(AktivitetEntityDto::getAktivitetsType)
                .map(AktivitetsType::name)
                .collect(Collectors.toSet());
        Set<String> aktiveTiltak = avtalteAktivteter.stream()
                .map(AktivitetEntityDto::getMuligTiltaksNavn)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        byggAktivitetStatusBrukerData(entity, avtalteAktivteter);
        byggAktivitetBrukerData(entity, avtalteAktivteter);
        return entity
                .setAktiviteter(aktiveAktiviteter)
                .setTiltak(aktiveTiltak);
    }

    private static void byggAktivitetStatusBrukerData(AktivitetStatusData aktivitetStatusData, List<AktivitetEntityDto> aktiviter) {
        LocalDate idag = LocalDate.now();

        Timestamp moteFremtidigStartdato = null;
        Timestamp moteFremtidigUtlopsdato = null;
        Timestamp stillingFremtidigUtlopsdato = null;
        Timestamp egenFremtidigUtlopsdato = null;
        Timestamp behandlingFremtidigUtlopsdato = null;
        Timestamp ijobbFremtidigUtlopsdato = null;
        Timestamp sokeavtaleFremtidigUtlopsdato = null;
        Timestamp tiltakFremtidigUtlopsdato = null;
        Timestamp utdanningaktivitetFremtidigUtlopsdato = null;
        Timestamp gruppeaktivitetFremtidigUtlopsdato = null;

        for (AktivitetEntityDto aktivitet : aktiviter) {
            if (aktivitet.getAktivitetsType().equals(mote) && !(aktivitet.getStart() == null || aktivitet.getStart().toLocalDateTime().toLocalDate().isBefore(idag))) {
                moteFremtidigStartdato = nesteFremITiden(moteFremtidigStartdato, aktivitet.getStart());
            }
            if (aktivitet.getUtlop() == null || idag.isAfter(aktivitet.getUtlop().toLocalDateTime().toLocalDate())) {
                continue;
            }
            switch (aktivitet.getAktivitetsType()) {
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
        aktivitetStatusData
                .setAktivitetMoteStartdato(toIsoUTC(moteFremtidigStartdato))
                .setAktivitetEgenUtlopsdato(Optional.ofNullable(toIsoUTC(egenFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetStillingUtlopsdato(Optional.ofNullable(toIsoUTC(stillingFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetMoteUtlopsdato(Optional.ofNullable(toIsoUTC(moteFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetBehandlingUtlopsdato(Optional.ofNullable(toIsoUTC(behandlingFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetIjobbUtlopsdato(Optional.ofNullable(toIsoUTC(ijobbFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetSokeavtaleUtlopsdato(Optional.ofNullable(toIsoUTC(sokeavtaleFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetTiltakUtlopsdato(Optional.ofNullable(toIsoUTC(tiltakFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetUtdanningaktivitetUtlopsdato(Optional.ofNullable(toIsoUTC(utdanningaktivitetFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()))
                .setAktivitetGruppeaktivitetUtlopsdato(Optional.ofNullable(toIsoUTC(gruppeaktivitetFremtidigUtlopsdato))
                        .orElse(getFarInTheFutureDate()));
    }

    private static void byggAktivitetBrukerData(AvtaltAktivitetEntity avtaltAktivitetEntity, List<AktivitetEntityDto> alleAktiviter) {
        LocalDate idag = LocalDate.now();

        List<Timestamp> startDatoer = alleAktiviter.stream().map(AktivitetEntityDto::getStart).filter(Objects::nonNull).toList();
        List<Timestamp> sluttdatoer = alleAktiviter.stream().map(AktivitetEntityDto::getUtlop).filter(Objects::nonNull).toList();

        Optional<Timestamp> nyesteUtlopteDato = Optional.ofNullable(finnNyesteUtlopteAktivAktivitet(sluttdatoer, idag));
        Optional<Timestamp> forrigeAktivitetStart = Optional.ofNullable(finnForrigeAktivitetStartDatoer(startDatoer, idag));

        List<Timestamp> startDatoerEtterDagensDato = finnDatoerEtterDagensDato(startDatoer, idag);
        Optional<Timestamp> aktivitetStart = (startDatoerEtterDagensDato.isEmpty()) ? Optional.empty() : Optional.ofNullable(startDatoerEtterDagensDato.get(0));
        Optional<Timestamp> nesteAktivitetStart = (startDatoerEtterDagensDato.size() < 2) ? Optional.empty() : Optional.ofNullable(startDatoerEtterDagensDato.get(1));

        avtaltAktivitetEntity.setAktivitetStart(aktivitetStart.map(DateUtils::toIsoUTC).orElse(null))
                .setNesteAktivitetStart(nesteAktivitetStart.map(DateUtils::toIsoUTC).orElse(null))
                .setNyesteUtlopteAktivitet(nyesteUtlopteDato.map(DateUtils::toIsoUTC).orElse(null))
                .setForrigeAktivitetStart(forrigeAktivitetStart.map(DateUtils::toIsoUTC).orElse(null));
    }

    private static Timestamp nesteFremITiden(Timestamp a, Timestamp b) {
        return a == null ? b : (b == null ? a : (a.before(b) ? a : b));
    }
}
