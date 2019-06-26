package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.fo.veilarbportefolje.domene.AktivitetStatus;
import no.nav.fo.veilarbportefolje.domene.Fnr;
import no.nav.fo.veilarbportefolje.domene.PersonId;
import no.nav.fo.veilarbportefolje.domene.aktivitet.AktivitetTyper;
import no.nav.fo.veilarbportefolje.indeksering.domene.OppfolgingsBruker;
import no.nav.fo.veilarbportefolje.util.AktivitetUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IndekseringUtils {

    static String createIndexName(String alias) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", alias, timestamp);
    }

    static OppfolgingsBruker finnBruker(List<OppfolgingsBruker> brukere, Fnr fnr) {
        return brukere.stream()
                .filter(bruker -> bruker.getFnr().equals(fnr.toString()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    static OppfolgingsBruker finnBruker(List<OppfolgingsBruker> brukere, PersonId personId) {
        return brukere.stream()
                .filter(bruker -> bruker.getPerson_id().equals(personId.toString()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    static void leggTilUtlopsDato(OppfolgingsBruker bruker, AktivitetStatus status) {
        String utlop = AktivitetUtils.statusToIsoUtcString(status);
        AktivitetTyper type = AktivitetTyper.valueOf(status.getAktivitetType());
        switch (type) {
            case egen:
                bruker.setAktivitet_egen_utlopsdato(utlop);
                break;
            case stilling:
                bruker.setAktivitet_stilling_utlopsdato(utlop);
                break;
            case sokeavtale:
                bruker.setAktivitet_sokeavtale_utlopsdato(utlop);
                break;
            case behandling:
                bruker.setAktivitet_behandling_utlopsdato(utlop);
                break;
            case ijobb:
                bruker.setAktivitet_ijobb_utlopsdato(utlop);
                break;
            case mote:
                bruker.setAktivitet_mote_utlopsdato(utlop);
                break;
            case tiltak:
                bruker.setAktivitet_tiltak_utlopsdato(utlop);
                break;
            case gruppeaktivitet:
                bruker.setAktivitet_gruppeaktivitet_utlopsdato(utlop);
                break;
            case utdanningaktivitet:
                bruker.setAktivitet_utdanningaktivitet_utlopsdato(utlop);
                break;
            default:
                throw new IllegalStateException("Fant ikke riktig aktivitetstype");
        }
    }

    static void leggTilStartDato(OppfolgingsBruker bruker, AktivitetStatus status) {
        String start = AktivitetUtils.startDatoToIsoUtcString(status);
        AktivitetTyper type = AktivitetTyper.valueOf(status.getAktivitetType());
        switch (type) {
            case mote:
                bruker.setAktivitet_mote_startdato(start);
                break;
            default:
                return;
        }
    }
}
