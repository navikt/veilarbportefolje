package no.nav.pto.veilarbportefolje.tiltaksaktivitet;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

@Slf4j
public class Tiltakskodeverk {

    public static final Map<String, String> tiltakskodeTiltaksnavnMap = new HashMap<>(
            Map.ofEntries(
                    entry("MIDLONTIL", "Midlertidig lønnstilskudd"),
                    entry("VARLONTIL", "Varig lønnstilskudd"),
                    entry("ARBTREN", "Arbeidstrening"),
                    entry("MENTOR", "Mentor"),
                    entry("VATIAROR", "Varig tilrettelagt arbeid i ordinær virksomhet"),
                    entry("INDOPPFAG", "Oppfølging"),
                    entry("ARBFORB", "Arbeidsforberedende trening"),
                    entry("AVKLARAG", "Avklaring"),
                    entry("VASV", "Varig tilrettelagt arbeid i skjermet virksomhet"),
                    entry("ARBRRHDAG", "Arbeidsrettet rehabilitering"),
                    entry("JOBBK", "Jobbklubb"),
                    entry("ENKELAMO", "Enkeltplass AMO"),
                    entry("DIGIOPPARB", "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)"),
                    entry("ENKFAGYRKE", "Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning"),
                    entry("HOYEREUTD", "Høyere utdanning"),
                    entry("GRUPPEAMO", "Gruppe AMO"),
                    entry("GRUFAGYRKE", "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning"),
                    entry("ARBEIDSMARKEDSOPPLAERING", "Arbeidsmarkedsopplæring (AMO)"),
                    entry("NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV", "Norskopplæring, grunnleggende ferdigheter og FOV"),
                    entry("STUDIESPESIALISERING", "Studiespesialisering"),
                    entry("FAG_OG_YRKESOPPLAERING", "Fag- og yrkesopplæring"),
                    entry("HOYERE_YRKESFAGLIG_UTDANNING", "Fagskole (høyere yrkesfaglig utdanning)"),
                    entry("FIREARIG_LONNSTILSKUDD", "Fireårig lønnstilskudd for unge"),
                    entry("SOMMERJOBB", "Sommerjobb"),
                    entry("TILPASSET_JOBBSTOTTE","Tilpasset jobbstøtte")
            )
    );

    public static String mapTiltakskodeTilNavn(String tiltakkode) {
        return tiltakskodeTiltaksnavnMap.getOrDefault(tiltakkode, null);
    }
}
