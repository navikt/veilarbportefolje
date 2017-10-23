package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.UtdanningaktivitetTyper;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.fo.util.AktivitetUtils.etterFilterDato;

public class TiltakUtils {

    final static String tiltak = "tiltak";
    final static String gruppeaktivitet = "gruppeaktivitet";
    final static String utdanningaktivitet = "utdanningaktivitet";

    static AktivitetStatus utledAktivitetstatusForTiltak(Bruker bruker, PersonId personId, Timestamp datofilterTiltak) {
        List<Tiltaksaktivitet> tiltaksAktiviteterEtterDatoFilter =
            bruker.getTiltaksaktivitetListe()
                .stream()
                .filter(tiltaksaktivitet -> etterFilterDato(hentUtlopsdatoForTiltak(tiltaksaktivitet),datofilterTiltak))
                .collect(Collectors.toList());

        if(tiltaksAktiviteterEtterDatoFilter.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = tiltaksAktiviteterEtterDatoFilter
                .stream()
                .map(TiltakUtils::hentUtlopsdatoForTiltak)
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), tiltak, true, nesteUtlopsdato);
    }

    static AktivitetStatus utledGruppeaktivitetstatus(Bruker bruker, PersonId personId, Timestamp datofilter) {
        List<Moeteplan> gruppeAktiviteterEtterDatoFilter =
            bruker.getGruppeaktivitetListe()
                .stream()
                .map(Gruppeaktivitet::getMoeteplanListe)
                .flatMap(Collection::stream)
                .filter(moeteplan -> etterFilterDato(tilTimestamp(moeteplan.getSluttDato()), datofilter))
                .collect(Collectors.toList());


        if(gruppeAktiviteterEtterDatoFilter.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoForMoteplan(gruppeAktiviteterEtterDatoFilter).orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), gruppeaktivitet, true, nesteUtlopsdato);
    }

    static AktivitetStatus utledUtdanningsaktivitetstatus(Bruker bruker, PersonId personId, Timestamp datofilter) {
        List<Utdanningsaktivitet> utdanningsaktiviteterEtterDato = bruker.getUtdanningsaktivitetListe()
                .stream()
                .filter(aktivitet -> UtdanningaktivitetTyper.contains(aktivitet.getAktivitetstype()))
                .filter(aktivitet -> etterFilterDato(tilTimestamp(aktivitet.getAktivitetPeriode().getTom()), datofilter))
                .collect(Collectors.toList());

        if(utdanningsaktiviteterEtterDato.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoUtdanningsaktiviteter(utdanningsaktiviteterEtterDato).orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), utdanningaktivitet, true, nesteUtlopsdato );

    }

    private static Optional<Timestamp> finnNesteUtlopsdatoUtdanningsaktiviteter(List<Utdanningsaktivitet> utdanningaktiviteter) {
        return utdanningaktiviteter
                .stream()
                .filter(Objects::nonNull)
                .map(Utdanningsaktivitet::getAktivitetPeriode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp)
                .sorted()
                .findFirst();
    }

    public static Optional<Timestamp> finnNesteUtlopsdatoForMoteplan(List<Moeteplan> moteplan) {
        return moteplan
                .stream()
                .filter(Objects::nonNull)
                .map(Moeteplan::getSluttDato)
                .map(TiltakUtils::tilTimestamp)
                .sorted()
                .findFirst();
    }

    public static Timestamp tilTimestamp(XMLGregorianCalendar calendar) {
        return Optional.of(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .map(Date::getTime)
                .map(Timestamp::new)
                .orElse(null);
    }

    public static Timestamp utledTildato(Periode periode) {
        return Optional.ofNullable(periode).map(deltagelsePeriode ->
                Optional.ofNullable(deltagelsePeriode.getTom())
                        .map(TiltakUtils::tilTimestamp)
                        .orElse(null))
                .orElse(null);
    }

    static Timestamp hentUtlopsdatoForTiltak(Tiltaksaktivitet tiltaksaktivitet) {
        return Optional.of(tiltaksaktivitet)
                .map(Tiltaksaktivitet::getDeltakelsePeriode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp)
                .orElse(null);
    }
}
