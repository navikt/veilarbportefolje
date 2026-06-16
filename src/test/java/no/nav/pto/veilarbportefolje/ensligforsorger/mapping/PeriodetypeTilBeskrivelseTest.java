package no.nav.pto.veilarbportefolje.ensligforsorger.mapping;

import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class PeriodetypeTilBeskrivelseTest {

    @Test
    public void migrering_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Migrering fra Infotrygd", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.MIGRERING));
    }

    @Test
    public void forlengelse_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Forlengelse", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.FORLENGELSE));
    }

    @Test
    public void hovedperiode_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Hovedperiode", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.HOVEDPERIODE));
    }

    @Test
    public void sanksjon_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Sanksjon", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.SANKSJON));
    }

    @Test
    public void periodeFørFødsel_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Periode før fødsel", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.PERIODE_FØR_FØDSEL));
    }

    @Test
    public void utvidelse_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Utvidelse", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.UTVIDELSE));
    }

    @Test
    public void nyPeriodeForNyttBarn_skalGiRiktigBeskrivelse() {
        Assert.assertEquals("Ny periode for nytt barn", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.NY_PERIODE_FOR_NYTT_BARN));
    }

//    @Test
//    public void særligTilsynskrevendeBarn_skalGiRiktigBeskrivelse() {
//        Assert.assertEquals("Særlig tilsynskrevende barn", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.SÆRLIG_TILSYNSKREVENDE_BARN));
//    }
//
//    @Test
//    public void barnUnder14Måneder_skalGiRiktigBeskrivelse() {
//        Assert.assertEquals("Barn under 14 måneder", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.BARN_UNDER_14_MÅNEDER));
//    }
//
//    @Test
//    public void forbigåendeSykdomHosBarnet_skalGiRiktigBeskrivelse() {
//        Assert.assertEquals("Forbigående sykdom hos barnet", PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse(Periodetype.FORBIGÅENDE_SYKDOM_HOS_BARNET));
//    }
}
