package no.nav.pto.veilarbportefolje.persononinfo.personopprinelse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Landgruppe {

    private static Landgruppe instance;
    private Map<String, String> landGruppeMap = new HashMap<>();

    public static Landgruppe getInstance() {
        if (instance == null) {
            instance = new Landgruppe();
        }
        return instance;
    }

    private Landgruppe() {
        List<String> landGruppe0 = getLandgruppeFromFile(0);
        landGruppe0.forEach(land -> landGruppeMap.put(land, "0"));

        List<String> landGruppe1 = getLandgruppeFromFile(1);
        landGruppe1.forEach(land -> landGruppeMap.put(land, "1"));

        List<String> landGruppe2 = getLandgruppeFromFile(2);
        landGruppe2.forEach(land -> landGruppeMap.put(land, "2"));

        List<String> landGruppe3 = getLandgruppeFromFile(3);
        landGruppe3.forEach(land -> landGruppeMap.put(land, "3"));
    }

    public String getLandgruppeForLandKode(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return null;
        }

        String ucCountryCode = countryCode.toUpperCase();
        return landGruppeMap.getOrDefault(ucCountryCode, null);
    }

    @SneakyThrows
    private List<String> getLandgruppeFromFile(Integer landGruppe) {
        ObjectMapper mapper = new ObjectMapper();
        String json = Optional.ofNullable(getClass()
                        .getResourceAsStream("/landgruppe/landgruppe" + landGruppe + ".json"))
                .map(this::readJsonFromFileStream)
                .orElseThrow();
        return mapper.readValue(json, new TypeReference<>() {
        });
    }

    @SneakyThrows
    private String readJsonFromFileStream(InputStream landGruppe) {
        return IOUtils.toString(landGruppe, String.valueOf(StandardCharsets.UTF_8));
    }

}
