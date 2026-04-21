package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

@Slf4j
public class TiltakkodeverkMapper {

    public static final Map<String, String> tiltakskodeTiltaksnavnMap = new HashMap<>(
            Map.ofEntries(
                    entry("MIDLONTIL", "Midlertidig lønnstilskudd"),
                    entry("VARLONTIL", "Varig lønnstilskudd"),
                    entry("SOMMERJOBB", "Sommerjobb"),
                    entry("FIREARIG_LONNSTILSKUDD", "Fireårig lønnstilskudd for unge")
            )
    );

    public static String mapTilTiltaknavn(String tiltakkode) {
        if (tiltakskodeTiltaksnavnMap.containsKey(tiltakkode)) {
            return tiltakskodeTiltaksnavnMap.get(tiltakkode);
        } else {
            return null;
        }
    }
}
