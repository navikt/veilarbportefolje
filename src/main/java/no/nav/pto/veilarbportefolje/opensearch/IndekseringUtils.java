package no.nav.pto.veilarbportefolje.opensearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;

import java.util.List;

import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyper.mote;

@Slf4j
public class IndekseringUtils {

    static OppfolgingsBruker finnBruker(List<OppfolgingsBruker> brukere, AktorId aktorId) {
        return brukere.stream()
                .filter(bruker -> aktorId.toString().equals(bruker.getAktoer_id()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
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
            case egen -> bruker.setAktivitet_egen_utlopsdato(utlop);
            case stilling -> bruker.setAktivitet_stilling_utlopsdato(utlop);
            case sokeavtale -> bruker.setAktivitet_sokeavtale_utlopsdato(utlop);
            case behandling -> bruker.setAktivitet_behandling_utlopsdato(utlop);
            case ijobb -> bruker.setAktivitet_ijobb_utlopsdato(utlop);
            case mote -> bruker.setAktivitet_mote_utlopsdato(utlop);
            case tiltak -> bruker.setAktivitet_tiltak_utlopsdato(utlop);
            case gruppeaktivitet -> bruker.setAktivitet_gruppeaktivitet_utlopsdato(utlop);
            case utdanningaktivitet -> bruker.setAktivitet_utdanningaktivitet_utlopsdato(utlop);
            default -> throw new IllegalStateException("Fant ikke riktig aktivitetstype");
        }
    }

    static void leggTilStartDato(OppfolgingsBruker bruker, AktivitetStatus status) {
        String start = AktivitetUtils.startDatoToIsoUtcString(status);
        AktivitetTyper type = AktivitetTyper.valueOf(status.getAktivitetType());
        if (type.equals(mote)) {
            bruker.setAktivitet_mote_startdato(start);
        }
    }
}
