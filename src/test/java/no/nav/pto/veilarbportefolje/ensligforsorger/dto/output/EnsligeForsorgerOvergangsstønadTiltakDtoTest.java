package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output;

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomLocalDate;

public class EnsligeForsorgerOvergangsstønadTiltakDtoTest {
    @Test
    public void testCastingToOpensearchDto() {
        EnsligeForsorgerOvergangsstønadTiltakDto ensligeForsorgereTiltak = new EnsligeForsorgerOvergangsstønadTiltakDto("Hovedperiode", false, randomLocalDate(), randomLocalDate());
        EnsligeForsorgereOvergangsstonad ensligeForsorgereOpensearchDto = ensligeForsorgereTiltak.toEnsligeForsorgereOpensearchDto();

        Assert.assertEquals(ensligeForsorgereOpensearchDto.vedtaksPeriodetype(), ensligeForsorgereTiltak.vedtaksPeriodetypeBeskrivelse());
        Assert.assertEquals(ensligeForsorgereOpensearchDto.harAktivitetsplikt(), ensligeForsorgereTiltak.aktivitsplikt());
        Assert.assertEquals(ensligeForsorgereOpensearchDto.yngsteBarnsFødselsdato(), ensligeForsorgereTiltak.yngsteBarnsFødselsdato());
        Assert.assertEquals(ensligeForsorgereOpensearchDto.utlopsDato(), ensligeForsorgereTiltak.utløpsDato());

    }

}