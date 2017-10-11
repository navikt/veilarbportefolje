package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class TiltakUtils {

    final static String tiltak = "tiltak";
    final static String gruppeaktivitet = "gruppeaktivitet";

    static AktivitetStatus utledAktivitetstatusForTiltak(Bruker bruker, PersonId personId, Timestamp datofilterTiltak) {
        List<Tiltaksaktivitet> tiltaksAktiviteterEtterDatoFilter =
            bruker.getTiltaksaktivitetListe()
                .stream()
                .filter(tiltaksaktivitet -> AktivitetUtils.etterFilterDato(hentUtlopsdatoForTiltak(tiltaksaktivitet),datofilterTiltak))
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
                .filter(moeteplan -> AktivitetUtils.etterFilterDato(tilTimestamp(moeteplan.getSluttDato()), datofilter))
                .collect(Collectors.toList());


        if(gruppeAktiviteterEtterDatoFilter.isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = finnNesteUtlopsdatoForMoteplan(gruppeAktiviteterEtterDatoFilter);

        return AktivitetStatus.of(personId, AktoerId.of(null), gruppeaktivitet, true, nesteUtlopsdato);
    }

    public static Timestamp finnNesteUtlopsdatoForMoteplan(List<Moeteplan> moteplan) {
        return moteplan
                .stream()
                .filter(Objects::nonNull)
                .map(Moeteplan::getSluttDato)
                .map(TiltakUtils::tilTimestamp)
                .sorted()
                .findFirst()
                .orElse(null);
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
