package no.nav.pto.veilarbportefolje.postgres;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;
import no.nav.pto.veilarbportefolje.postgres.utils.AktivitetStatusData;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.*;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType.*;

public class PostgresAktivitetMapper {
    public static AktivitetEntity kalkulerGenerellAktivitetInformasjon(List<AktivitetEntityDto> aktiviteter) {
        AktivitetEntity entity = new AktivitetEntity();
        if (aktiviteter == null) {
            return entity;
        }
        Set<String> aktiveAktiviteter = aktiviteter.stream()
                .map(AktivitetEntityDto::getAktivitetsType)
                .map(AktivitetsType::name)
                .collect(Collectors.toSet());
        byggAktivitetStatusBrukerData(entity, aktiviteter);
        byggStillingFraNavData(aktiviteter,aktiveAktiviteter,entity);

        return entity.setAlleAktiviteter(aktiveAktiviteter);
    }

    public static AvtaltAktivitetEntity kalkulerAvtalteAktivitetInformasjon(List<AktivitetEntityDto> avtalteAktivteter) {
        AvtaltAktivitetEntity entity = new AvtaltAktivitetEntity();
        if (avtalteAktivteter == null) {
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
                .setAktivitetMoteStartdato(statusToIsoUtcString(moteFremtidigStartdato))
                .setAktivitetEgenUtlopsdato(statusToIsoUtcString(egenFremtidigUtlopsdato))
                .setAktivitetStillingUtlopsdato(statusToIsoUtcString(stillingFremtidigUtlopsdato))
                .setAktivitetMoteUtlopsdato(statusToIsoUtcString(moteFremtidigUtlopsdato))
                .setAktivitetBehandlingUtlopsdato(statusToIsoUtcString(behandlingFremtidigUtlopsdato))
                .setAktivitetIjobbUtlopsdato(statusToIsoUtcString(ijobbFremtidigUtlopsdato))
                .setAktivitetSokeavtaleUtlopsdato(statusToIsoUtcString(sokeavtaleFremtidigUtlopsdato))
                .setAktivitetTiltakUtlopsdato(statusToIsoUtcString(tiltakFremtidigUtlopsdato))
                .setAktivitetUtdanningaktivitetUtlopsdato(statusToIsoUtcString(utdanningaktivitetFremtidigUtlopsdato))
                .setAktivitetGruppeaktivitetUtlopsdato(statusToIsoUtcString(gruppeaktivitetFremtidigUtlopsdato));
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

    private static void byggStillingFraNavData(List<AktivitetEntityDto> aktiviteter, Set<String> aktiveAktiviteter, AktivitetEntity entity){
        if(aktiveAktiviteter.contains(stilling_fra_nav.name())){
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Optional<AktivitetEntityDto> nesteStillingFraNav = aktiviteter.stream()
                    .filter(aktivitetEntityDto -> stilling_fra_nav.equals(aktivitetEntityDto.aktivitetsType))
                    .filter(aktivitetEntityDto -> aktivitetEntityDto.svarfristStillingFraNav != null)
                    .filter(aktivitetEntityDto -> LocalDate.parse(aktivitetEntityDto.svarfristStillingFraNav.substring(0,10)).isAfter(yesterday))
                    .min(Comparator.comparing(frist -> frist.svarfristStillingFraNav));
            entity.setNesteCvKanDelesStatus(nesteStillingFraNav.map(AktivitetEntityDto::getCvKanDelesStatus).orElse(null));
            entity.setNesteSvarfristStillingFraNav(nesteStillingFraNav.map((AktivitetEntityDto::getSvarfristStillingFraNav)).orElse(null));
        }
    }

    private static Timestamp nesteFremITiden(Timestamp a, Timestamp b) {
        return a == null ? b : (b == null ? a : (a.before(b) ? a : b));
    }
}
