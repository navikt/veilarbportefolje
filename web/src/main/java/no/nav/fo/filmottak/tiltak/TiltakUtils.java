package no.nav.fo.filmottak.tiltak;

import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class TiltakUtils {

    final static String tiltak = "tiltak";
    final static String gruppeaktivitet = "gruppeaktivitet";

    static AktivitetStatus utledAktivitetstatusForTiltak(Bruker bruker, PersonId personId, Timestamp datofilterTiltak) {
        if(bruker.getTiltaksaktivitetListe().isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = bruker
                .getTiltaksaktivitetListe()
                .stream()
                .map(TiltakUtils::hentUtlopsdatoForTiltak)
                .filter(Objects::nonNull)
                .filter(timestamp -> AktivitetUtils.etterFilterDato(timestamp, datofilterTiltak))
                .sorted()
                .findFirst()
                .orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), tiltak, true, nesteUtlopsdato);
    }

    static AktivitetStatus utledGruppeaktivitetstatus(Bruker bruker, PersonId personId, Timestamp datofilter) {
        if(bruker.getGruppeaktivitetListe().isEmpty()) {
            return null;
        }

        Timestamp nesteUtlopsdato = bruker.getGruppeaktivitetListe()
                .stream()
                .map(Gruppeaktivitet::getMoeteplanListe)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(Moeteplan::getSluttDato)
                .map(TiltakUtils::tilTimestamp)
                .filter(timestamp -> AktivitetUtils.etterFilterDato(timestamp, datofilter))
                .sorted()
                .findFirst()
                .orElse(null);

        return AktivitetStatus.of(personId, AktoerId.of(null), gruppeaktivitet, true, nesteUtlopsdato);
    }

    static Timestamp tilTimestamp(XMLGregorianCalendar calendar) {
        return Optional.of(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .map(Date::getTime)
                .map(Timestamp::new)
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
