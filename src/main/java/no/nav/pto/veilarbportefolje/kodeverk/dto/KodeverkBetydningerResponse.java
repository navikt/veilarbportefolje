package no.nav.pto.veilarbportefolje.kodeverk.dto;

import java.util.List;
import java.util.Map;

public record KodeverkBetydningerResponse(Map<String, List<KodeverkBetydning>> betydninger) {

    public record KodeverkBetydning(String gyldigFra, Map<String, KodeverkBeskrivelse> beskrivelser) {}

    public record KodeverkBeskrivelse(String tekst, String term) {}
}
