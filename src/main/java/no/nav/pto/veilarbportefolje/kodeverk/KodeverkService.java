package no.nav.pto.veilarbportefolje.kodeverk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
public class KodeverkService {

    public final static String KODEVERK_LANDKODER = "Landkoder";
    public final static String KODEVERK_SPRAAK = "Språk";

    public final static String KODEVERK_BYDELER = "Bydeler";

    public final static String KODEVERK_KOMMUNER = "Kommuner";
    private final KodeverkClient kodeverkClient;

    public String getBeskrivelseForLandkode(String kode) {
        if (kode != null && kode.equals("XXA")) return "statsløs";
        return finnBeskrivelse(KODEVERK_LANDKODER, kode);
    }

    public String getBeskrivelseForSpraakKode(String spraakKode) {
        return finnBeskrivelse(KODEVERK_SPRAAK, spraakKode);
    }

    public String getBeskrivelseForBydeler(String bydel) {
        return finnBeskrivelse(KODEVERK_BYDELER, bydel);
    }

    public String getBeskrivelseForKomunner(String komunne) {
        return finnBeskrivelse(KODEVERK_KOMMUNER, komunne);
    }

    private String finnBeskrivelse(String kodeverksnavn, String kode) {
        if (StringUtils.nullOrEmpty(kode)) {
            return "";
        }
        Map<String, String> betydninger = kodeverkClient.hentKodeverkBeskrivelser(kodeverksnavn);
        String betydning = betydninger.get(kode);

        if (betydning == null) {
            log.error(format("Fant ikke kode %s i kodeverk %s", kode, kodeverksnavn));
            return "Ikke tilgjengelig";
        }

        return betydning;
    }

}
