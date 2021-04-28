package no.nav.pto.veilarbportefolje.postgres;

import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.UFORDELTE_BRUKERE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresServiceTest {
    private PostgresService postgresService;
    private VeilarbVeilederClient veilarbVeilederClient;

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        VedtakstottePilotRequest vedtakstottePilotRequest =  mock(VedtakstottePilotRequest.class);
        veilarbVeilederClient = mock(VeilarbVeilederClient.class);

        postgresService = new PostgresService(vedtakstottePilotRequest, db, veilarbVeilederClient);
    }

    @Test
    public void sok_resulterer_i_ingen_brukere(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));

        Filtervalg filtervalg = new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE));
        postgresService.hentBrukere("1234",null, null,null, filtervalg, 0, 10);

    }
}
