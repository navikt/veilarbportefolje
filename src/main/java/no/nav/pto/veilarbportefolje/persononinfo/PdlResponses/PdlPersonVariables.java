package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.AllArgsConstructor;
import lombok.Data;

public class PdlPersonVariables {
    @Data
    @AllArgsConstructor
    public static class HentFodselsdag {
        private String ident;
    }
}
