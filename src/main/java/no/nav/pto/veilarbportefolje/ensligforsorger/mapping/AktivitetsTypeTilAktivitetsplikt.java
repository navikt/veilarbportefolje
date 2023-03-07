package no.nav.pto.veilarbportefolje.ensligforsorger.mapping;

import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;

import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype.*;
import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype.MIGRERING;
import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype.*;

public class AktivitetsTypeTilAktivitetsplikt {
    public static Optional<Boolean> harAktivitetsplikt(Periodetype periodetype, Aktivitetstype aktivitetstypes) {
        if (periodetype.equals(PERIODE_FØR_FØDSEL)) {
            return Optional.of(false);
        }

        if (periodetype.equals(HOVEDPERIODE) || periodetype.equals(NY_PERIODE_FOR_NYTT_BARN)) {
            if (aktivitetstypes.equals(BARN_UNDER_ETT_ÅR)) {
                return Optional.of(false);
            }

            if (List.of(FORSØRGER_I_ARBEID, FORSØRGER_REELL_ARBEIDSSØKER, FORSØRGER_I_UTDANNING,
                            FORSØRGER_ETABLERER_VIRKSOMHET)
                    .contains(aktivitetstypes)) {
                return Optional.of(true);
            }

            if (List.of(BARNET_SÆRLIG_TILSYNSKREVENDE, FORSØRGER_MANGLER_TILSYNSORDNING,
                            FORSØRGER_ER_SYK, BARNET_ER_SYKT)
                    .contains(aktivitetstypes)) {
                return Optional.of(false);
            }
        }

        if (periodetype.equals(UTVIDELSE)) {
            if (aktivitetstypes.equals(UTVIDELSE_FORSØRGER_I_UTDANNING)) {
                return Optional.of(true);
            }

            if (aktivitetstypes.equals(UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE)) {
                return Optional.of(false);
            }
        }

        if (periodetype.equals(FORLENGELSE)) {
            if (aktivitetstypes.equals(FORLENGELSE_STØNAD_UT_SKOLEÅRET)) {
                return Optional.of(true);
            }

            if (List.of(FORLENGELSE_MIDLERTIDIG_SYKDOM, FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
                    FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
                    FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM,
                    FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING,
                    FORLENGELSE_STØNAD_PÅVENTE_UTDANNING).contains(aktivitetstypes)) {
                return Optional.of(false);
            }
        }

        if (periodetype.equals(SANKSJON)) {
            return Optional.of(false);
        }

        if (aktivitetstypes.equals(IKKE_AKTIVITETSPLIKT)) {
            return Optional.of(false);
        }

        if (periodetype.equals(MIGRERING) || aktivitetstypes.equals(Aktivitetstype.MIGRERING)) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
