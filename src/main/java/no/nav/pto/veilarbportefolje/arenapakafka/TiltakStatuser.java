package no.nav.pto.veilarbportefolje.arenapakafka;

import java.util.List;

/**
 GodkjenteStatuser: Aktuell (AKTUELL), Gjennomføres (GJENN), Informasjonsmøte (INFOMOETE), Takket ja til tilbud (JATAKK), Godkjent tiltaksplass (TILBUD), Venteliste (VENTELISTE)
 *  AKTUELL gjelder kun for Arbeidsmarkedopplæring (AMO) og er da unntatt dersom det gjelder Individuelt tiltak (IND) eller Institusjonelt tiltak (INST)
 *
 * @link https://confluence.adeo.no/pages/viewpage.action?pageId=409961201
 */
public class TiltakStatuser {
    public static List<String> godkjenteTiltaksStatuser = List.of("GJENN", "INFOMOETE", "JATAKK", "TILBUD", "VENTELISTE", "AKTUELL");
}
