package no.nav.pto.veilarbportefolje.ensligforsorger.mapping;

import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;

public class PeriodetypeTilBeskrivelse {
    public static String mapPeriodetypeTilBeskrivelse(Periodetype periodetype) {
        return switch (periodetype) {
            case MIGRERING -> "Migrering fra Infotrygd";
            case FORLENGELSE -> "Forlengelse";
            case HOVEDPERIODE -> "Hovedperiode";
            case SANKSJON -> "Sanksjon";
            case PERIODE_FØR_FØDSEL -> "Periode før fødsel";
            case UTVIDELSE -> "Utvidelse";
            case NY_PERIODE_FOR_NYTT_BARN -> "Ny periode for nytt barn";
        };
    }

}
