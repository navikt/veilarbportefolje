package no.nav.pto.veilarbportefolje.persononinfo.personopprinelse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Landgruppe {

    private static Landgruppe instance;
    private List<String> landGruppe0 = new ArrayList<>();
    private List<String> landGruppe1 = new ArrayList<>();
    private List<String> landGruppe2 = new ArrayList<>();
    private List<String> landGruppe3 = new ArrayList<>();

    public static Landgruppe getInstance() {
        if (instance == null) {
            instance = new Landgruppe();
        }
        return instance;
    }

    private Landgruppe() {
        landGruppe0.addAll(getLandgruppeFromFile(0));
        landGruppe1.addAll(getLandgruppeFromFile(1));
        landGruppe2.addAll(getLandgruppeFromFile(2));
        landGruppe3.addAll(getLandgruppeFromFile(3));
    }

    public String getLandgruppeForLandKode(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return null;
        }

        String ucCountryCode = countryCode.toUpperCase();
        if (landGruppe0.contains(ucCountryCode)) {
            return "0";
        } else if (landGruppe1.contains(ucCountryCode)) {
            return "1";
        } else if (landGruppe2.contains(ucCountryCode)) {
            return "2";
        } else if (landGruppe3.contains(ucCountryCode)) {
            return "3";
        }
        return null;
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
