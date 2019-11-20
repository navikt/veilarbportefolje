package no.nav.fo.veilarbportefolje.provider.rest;

import no.nav.brukerdialog.security.domain.IdentType;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;

import static no.nav.fo.veilarbportefolje.provider.rest.OppfolgingenhetRessurs.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class OppfolgingsenhetRessursTest {

    @Test
    public void skal_sjekke_om_page_number_er_storre_enn_totalt_antall_pages() {
        try {
            validatePageNumber(2, 1);
        } catch (WebApplicationException e) {
            int status = getStatus(e);
            assertThat(status).isEqualTo(404);
        }
    }

    @Test
    public void skal_sjekke_at_page_size_ikke_er_for_liten() {
        try {
            validatePageSize(0);
        } catch (WebApplicationException e) {
            int status = getStatus(e);
            assertThat(status).isEqualTo(400);
        }
    }

    @Test
    public void skal_sjekke_at_page_size_ikke_er_for_stor() {
        try {
            validatePageSize(PAGE_SIZE_MAX + 1);
        } catch (WebApplicationException e) {
            int status = getStatus(e);
            assertThat(status).isEqualTo(400);
        }
    }

    @Test
    public void skal_returnere_true_ved_ugyldig_ident() {
        boolean result = ugyldigIdent(IdentType.Systemressurs, "srvveilarbportefolje");
        assertThat(result).isTrue();
    }

    @Test
    public void skal_returnere_true_ved_ugyldig_ident_type() {
        boolean result = ugyldigIdent(IdentType.InternBruker, "srvveilarboppfolging");
        assertThat(result).isTrue();
    }

    private int getStatus(WebApplicationException e) {
        return e.getResponse().getStatus();
    }



}
