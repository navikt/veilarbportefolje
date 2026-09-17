package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output;

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad;
import org.junit.jupiter.api.Test;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomLocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnsligForsorgerOvergangsstonadTiltakDtoTest {

    @Test
    public void testCastingToOpensearchDto() {
        EnsligForsorgerOvergangsstonadTiltakDto ensligeForsorgereTiltak = new EnsligForsorgerOvergangsstonadTiltakDto("Hovedperiode", false, randomLocalDate(), randomLocalDate());
        EnsligeForsorgereOvergangsstonad ensligeForsorgereOpensearchDto = ensligeForsorgereTiltak.toEnsligeForsorgereOpensearchDto();

        assertEquals(ensligeForsorgereOpensearchDto.vedtaksPeriodetype(), ensligeForsorgereTiltak.vedtaksPeriodetypeBeskrivelse);
        assertEquals(ensligeForsorgereOpensearchDto.harAktivitetsplikt(), ensligeForsorgereTiltak.aktivitetsplikt);
        assertEquals(ensligeForsorgereOpensearchDto.yngsteBarnsFodselsdato(), ensligeForsorgereTiltak.yngsteBarnsFodselsdato);
        assertEquals(ensligeForsorgereOpensearchDto.utlopsDato(), ensligeForsorgereTiltak.utlopsDato);
    }
}
