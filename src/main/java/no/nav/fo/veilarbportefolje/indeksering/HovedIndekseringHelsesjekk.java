package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

@Component
public class HovedIndekseringHelsesjekk implements Helsesjekk {

    private ElasticSearchService elasticSearchService;

    public HovedIndekseringHelsesjekk(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    public void helsesjekk() {
        Exception indekseringFeilet = elasticSearchService.hentIndekseringFeiletStatus();
        if (indekseringFeilet != null) {
            throw new RuntimeException(indekseringFeilet);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "hovedindeksering",
                String.format("https://%s/%s", IndekseringConfig.getElasticUrl(), IndekseringConfig.getAlias()),
                "Sjekker om forrige hovedindeksering var vellykket",
                true
        );
    }
}
