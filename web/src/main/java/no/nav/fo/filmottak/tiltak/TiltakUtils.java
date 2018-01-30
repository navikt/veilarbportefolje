package no.nav.fo.filmottak.tiltak;

import io.vavr.control.Try;
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
import java.util.stream.Collectors;
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

        if (tiltaksAktiviteterEtterDatoFilter.isEmpty()) {
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


        if (gruppeAktiviteterEtterDatoFilter.isEmpty()) {
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

        if (utdanningsaktiviteterEtterDato.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoUtdanningsaktiviteter(utdanningsaktiviteterEtterDato).orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), utdanningaktivitet, true, nesteUtlopsdato);

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
        return Optional.ofNullable(calendar)
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

    public static Optional<Timestamp> utledFradato(Periode periode) {
        return Optional.ofNullable(periode)
                .map(Periode::getFom)
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
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .findFirst().orElse(null);
    }

    static Timestamp finnEldsteFOMForMoteplanliste(List<Moeteplan> moteplanliste) {
        return moteplanliste.stream()
                .map(Moeteplan::getStartDato)
                .map(TiltakUtils::tilTimestamp)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse(null);
    }

    public static TiltakOppdateringer finnOppdateringForBruker(Bruker bruker) {
        List<TiltakDatoer> tiltakDatoer = getTiltaksDatoer(bruker);

        Set<Timestamp> startDatoerEtterDagensDato = finnStartDatoerEtterDagensDato(tiltakDatoer);
        Iterator<Timestamp> iterator = startDatoerEtterDagensDato.iterator();

        Timestamp aktivitetStart = Try.of(iterator::next).getOrElse((Timestamp) null);
        Timestamp nesteAktivitetStart = Try.of(iterator::next).getOrElse((Timestamp) null);
        Timestamp forrigeAktivtetStart = finnForrigeAktivtetStartDato(tiltakDatoer);

        Timestamp nyesteUtlopsdato = finnNyesteUtlopsdato(tiltakDatoer);

        return TiltakOppdateringer
                .builder()
                .aktivitetStart(aktivitetStart)
                .nesteAktivitetStart(nesteAktivitetStart)
                .forrigeAktivitetStart(forrigeAktivtetStart)
                .nyesteUtlopteAktivitet(nyesteUtlopsdato)
                .build();
    }

    private static List<TiltakDatoer> getTiltaksDatoer(Bruker bruker) {
        return Stream.of(
                bruker.getTiltaksaktivitetListe().stream()
                        .map(TiltakUtils::mapTiltakOppdateringer),

                bruker.getGruppeaktivitetListe().stream()
                        .map(TiltakUtils::mapTiltakOppdateringer),

                bruker.getUtdanningsaktivitetListe().stream()
                        .map(TiltakUtils::mapTiltakOppdateringer)
        )
                .flatMap(Function.identity())
                .filter(oppdatering -> AktivitetUtils.etterFilterDato(oppdatering.getSluttDato().orElse(null)))
                .collect(toList());
    }

    private static Timestamp finnForrigeAktivtetStartDato(List<TiltakDatoer> tiltakDatoer) {
        return tiltakDatoer
                .stream()
                .map(tiltak -> tiltak.getStartDato().orElse(null))
                .filter(Objects::nonNull)
                .filter(not(TiltakUtils::fraOgMedDagensDato))
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElse(null);
    }

    private static LinkedHashSet<Timestamp> finnStartDatoerEtterDagensDato(List<TiltakDatoer> tiltakDatoer) {
        return tiltakDatoer
                .stream()
                .map(tiltak -> tiltak.getStartDato().orElse(null))
                .filter(Objects::nonNull)
                .filter(TiltakUtils::fraOgMedDagensDato)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Timestamp finnNyesteUtlopsdato(List<TiltakDatoer> tiltakDatoer) {
        return tiltakDatoer
                    .stream()
                    .map(tiltak -> tiltak.getSluttDato().orElse(null))
                    .filter(Objects::nonNull)
                    .filter(not(TiltakUtils::fraOgMedDagensDato))
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElse(null);
    }

    private static TiltakDatoer mapTiltakOppdateringer(Tiltaksaktivitet tiltak) {
        Periode deltakelsePeriode = tiltak.getDeltakelsePeriode();
        return TiltakDatoer.of(utledFradato(deltakelsePeriode), utledTildato(deltakelsePeriode));
    }

    private static TiltakDatoer mapTiltakOppdateringer(Utdanningsaktivitet tiltak) {
        Periode deltakelsePeriode = tiltak.getAktivitetPeriode();
        return TiltakDatoer.of(utledFradato(deltakelsePeriode), utledTildato(deltakelsePeriode));
    }

    private static TiltakDatoer mapTiltakOppdateringer(Gruppeaktivitet tiltak) {
        Optional<Timestamp> nyesteTOMDato = Optional.ofNullable(finnNyesteTOMForMoteplanliste(tiltak.getMoeteplanListe()));
        Optional<Timestamp> eldsteFOMDato = Optional.ofNullable(finnEldsteFOMForMoteplanliste(tiltak.getMoeteplanListe()));
        return TiltakDatoer.of(eldsteFOMDato, nyesteTOMDato);
    }
}
