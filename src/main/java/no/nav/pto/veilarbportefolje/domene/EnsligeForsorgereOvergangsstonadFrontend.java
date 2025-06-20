package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Objects;

public record EnsligeForsorgereOvergangsstonadFrontend(
        String vedtaksPeriodetype,
        Boolean harAktivitetsplikt,
        LocalDate utlopsDato,
        LocalDate yngsteBarnsFodselsdato) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EnsligeForsorgereOvergangsstonadFrontend {
    }

    public static EnsligeForsorgereOvergangsstonadFrontend of(EnsligeForsorgereOvergangsstonad ensligeForsorgereOvergangsstonad) {
        if (Objects.nonNull(ensligeForsorgereOvergangsstonad)) {
            return new EnsligeForsorgereOvergangsstonadFrontend(
                    ensligeForsorgereOvergangsstonad.vedtaksPeriodetype(),
                    ensligeForsorgereOvergangsstonad.harAktivitetsplikt(),
                    ensligeForsorgereOvergangsstonad.utlopsDato(),
                    ensligeForsorgereOvergangsstonad.yngsteBarnsFødselsdato()
            );
        }
        return null;
    }
}
