package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output;

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad;
import org.junit.jupiter.api.Test;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomLocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnsligeForsorgerOvergangsstønadTiltakDtoTest {

    @Test
    public void testCastingToOpensearchDto() {
        EnsligeForsorgerOvergangsstønadTiltakDto ensligeForsorgereTiltak = new EnsligeForsorgerOvergangsstønadTiltakDto("Hovedperiode", false, randomLocalDate(), randomLocalDate());
        EnsligeForsorgereOvergangsstonad ensligeForsorgereOpensearchDto = ensligeForsorgereTiltak.toEnsligeForsorgereOpensearchDto();

        assertEquals(ensligeForsorgereOpensearchDto.vedtaksPeriodetype(), ensligeForsorgereTiltak.vedtaksPeriodetypeBeskrivelse());
        assertEquals(ensligeForsorgereOpensearchDto.harAktivitetsplikt(), ensligeForsorgereTiltak.aktivitsplikt());
        assertEquals(ensligeForsorgereOpensearchDto.yngsteBarnsFødselsdato(), ensligeForsorgereTiltak.yngsteBarnsFødselsdato());
        assertEquals(ensligeForsorgereOpensearchDto.utlopsDato(), ensligeForsorgereTiltak.utløpsDato());
    }
}
