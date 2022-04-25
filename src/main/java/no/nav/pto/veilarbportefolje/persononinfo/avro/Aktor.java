package no.nav.pto.veilarbportefolje.persononinfo.avro;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Aktor {
    private List<Identifikator> identifikatorer;

    @Data
    @Accessors(chain = true)
    public static class Identifikator {
        private String idnummer;
        private Type type;
        private boolean gjeldende;
    }

    public enum Type {
        FOLKEREGISTERIDENT,
        AKTORID,
        NPID
    }
}
