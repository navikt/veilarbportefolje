package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.UtdanningaktivitetTyper;
import no.nav.fo.util.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.AktivitetUtils.etterFilterDato;
import static no.nav.fo.util.DbUtils.not;

public class TiltakUtils {

    final static String tiltak = "tiltak";
    final static String gruppeaktivitet = "gruppeaktivitet";
    final static String utdanningaktivitet = "utdanningaktivitet";

    static AktivitetStatus utledAktivitetstatusForTiltak(Bruker bruker, PersonId personId) {
        List<Tiltaksaktivitet> tiltaksAktiviteterEtterDatoFilter =
            bruker.getTiltaksaktivitetListe()
                .stream()
                .filter(tiltaksaktivitet -> etterFilterDato(hentUtlopsdatoForTiltak(tiltaksaktivitet)))
                .collect(toList());

        if(tiltaksAktiviteterEtterDatoFilter.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteDatoFraOgMedDagens(tiltaksAktiviteterEtterDatoFilter
                .stream()
                .map(TiltakUtils::hentUtlopsdatoForTiltak)
                .filter(Objects::nonNull)
                .collect(toList()))
                .orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), tiltak, true, nesteUtlopsdato);
    }

    static AktivitetStatus utledGruppeaktivitetstatus(Bruker bruker, PersonId personId) {
        List<Moeteplan> gruppeAktiviteterEtterDatoFilter =
            bruker.getGruppeaktivitetListe()
                .stream()
                .map(Gruppeaktivitet::getMoeteplanListe)
                .flatMap(Collection::stream)
                .filter(moeteplan -> etterFilterDato(tilTimestamp(moeteplan.getSluttDato())))
                .collect(toList());


        if(gruppeAktiviteterEtterDatoFilter.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoForMoteplan(gruppeAktiviteterEtterDatoFilter).orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), gruppeaktivitet, true, nesteUtlopsdato);
    }

    static AktivitetStatus utledUtdanningsaktivitetstatus(Bruker bruker, PersonId personId) {
        List<Utdanningsaktivitet> utdanningsaktiviteterEtterDato = bruker.getUtdanningsaktivitetListe()
                .stream()
                .filter(aktivitet -> UtdanningaktivitetTyper.contains(aktivitet.getAktivitetstype()))
                .filter(aktivitet -> etterFilterDato(tilTimestamp(aktivitet.getAktivitetPeriode().getTom())))
                .collect(toList());

        if(utdanningsaktiviteterEtterDato.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoUtdanningsaktiviteter(utdanningsaktiviteterEtterDato).orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), utdanningaktivitet, true, nesteUtlopsdato );

    }

    private static Optional<Timestamp> finnNesteUtlopsdatoUtdanningsaktiviteter(List<Utdanningsaktivitet> utdanningaktiviteter) {
        return finnNesteDatoFraOgMedDagens(utdanningaktiviteter
                .stream()
                .filter(Objects::nonNull)
                .map(Utdanningsaktivitet::getAktivitetPeriode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp)
                .collect(toList()));
    }

    public static Optional<Timestamp> finnNesteUtlopsdatoForMoteplan(List<Moeteplan> moteplan) {
        return finnNesteDatoFraOgMedDagens(moteplan
                .stream()
                .filter(Objects::nonNull)
                .map(Moeteplan::getSluttDato)
                .map(TiltakUtils::tilTimestamp)
                .collect(toList()));
    }

    private static Optional<Timestamp> finnNesteDatoFraOgMedDagens(List<Timestamp> timestamps) {
        return timestamps.stream()
                .filter(TiltakUtils::fraOgMedDagensDato)
                .sorted()
                .findFirst();
    }

    private static boolean fraOgMedDagensDato(Timestamp timestamp) {
        return LocalDate.now().isBefore(timestamp.toLocalDateTime().toLocalDate().plusDays(1));
    }

    public static Timestamp tilTimestamp(XMLGregorianCalendar calendar) {
        return Optional.of(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .map(Date::getTime)
                .map(Timestamp::new)
                .orElse(null);
    }

    public static Optional<Timestamp> utledTildato(Periode periode) {

        return Optional.ofNullable(periode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp);
    }

    static Timestamp hentUtlopsdatoForTiltak(Tiltaksaktivitet tiltaksaktivitet) {
        return Optional.of(tiltaksaktivitet)
                .map(Tiltaksaktivitet::getDeltakelsePeriode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp)
                .orElse(null);
    }

    static Timestamp finnNyesteTOMForMoteplanliste(List<Moeteplan> moteplanliste) {
        return moteplanliste.stream()
                .map(Moeteplan::getSluttDato)
                .map(TiltakUtils::tilTimestamp)
                .sorted(Comparator.reverseOrder())
                .findFirst().orElse(null);
    }

    public static Optional<Timestamp> finnNysteUtlopsdatoForBruker(Bruker bruker) {
        return Stream.of(
                bruker.getTiltaksaktivitetListe().stream()
                        .map(Tiltaksaktivitet::getDeltakelsePeriode)
                        .map(TiltakUtils::utledTildato)
                        .filter(Optional::isPresent)
                        .map(Optional::get),

                bruker.getGruppeaktivitetListe().stream()
                        .map(Gruppeaktivitet::getMoeteplanListe)
                        .map(TiltakUtils::finnNyesteTOMForMoteplanliste),

                bruker.getUtdanningsaktivitetListe().stream()
                        .map(Utdanningsaktivitet::getAktivitetPeriode)
                        .map(TiltakUtils::utledTildato)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
        ).flatMap(Function.identity())
                .filter(Objects::nonNull)
                .filter(AktivitetUtils::etterFilterDato)
                .filter(not(TiltakUtils::fraOgMedDagensDato))
                .sorted(Comparator.reverseOrder())
                .findFirst();
    }
}
