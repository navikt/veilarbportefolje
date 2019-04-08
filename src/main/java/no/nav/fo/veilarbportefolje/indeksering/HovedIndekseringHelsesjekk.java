package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.getAlias;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.getElasticHostname;


@Component
public class HovedIndekseringHelsesjekk implements Helsesjekk {

    private ElasticIndexer elasticIndexer;

    public HovedIndekseringHelsesjekk(ElasticIndexer elasticIndexer) {
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void helsesjekk() {
        Exception indekseringFeilet = elasticIndexer.hentIndekseringFeiletStatus();
        if (indekseringFeilet != null) {
            throw new RuntimeException(indekseringFeilet);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "hovedindeksering",
                String.format("https://%s/%s", getElasticHostname(), getAlias()),
                "Sjekker om forrige hovedindeksering var vellykket",
                true
        );
    }
}
