package no.nav.pto.veilarbportefolje.domene.frontendmodell;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad;

import java.time.LocalDate;
import java.util.Objects;


// Denne er like som EnsligeForsorgereOvergangsstonad men uten ø i Fødselsdato
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
