package no.nav.pto.veilarbportefolje.postgres;

import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;


public class PostgresService {
    private final VedtakstottePilotRequest vedtakstottePilotRequest;
    private final JdbcTemplate jdbcTemplate;

    public PostgresService(VedtakstottePilotRequest vedtakstottePilotRequest, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate) {
        this.vedtakstottePilotRequest = vedtakstottePilotRequest;
        this.jdbcTemplate = jdbcTemplate;
    }


    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        PostgresQueryBuilder query = new PostgresQueryBuilder(jdbcTemplate);

        boolean kallesFraMinOversikt = veilederIdent.isPresent() && StringUtils.isNotBlank(veilederIdent.get());
        if (kallesFraMinOversikt) {
            query.minOversikt(veilederIdent.get());
        }
        List<Bruker> result = query.search();

        return new BrukereMedAntall(result.size(), result);
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {
        return null;
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = this.erVedtakstottePilotPa(EnhetId.of(enhetId));
        return null;
    }

    public StatusTall hentStatusTallForEnhet(String enhetId) {
       return null;
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
         return null;
    }

    private boolean erVedtakstottePilotPa(EnhetId enhetId) {
        return vedtakstottePilotRequest.erVedtakstottePilotPa(enhetId);
    }
}
