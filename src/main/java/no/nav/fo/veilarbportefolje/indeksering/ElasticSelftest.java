package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.getAlias;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.getElasticHostname;

@Component
public class ElasticSelftest implements Helsesjekk {

    RestHighLevelClient client;

    private static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 300_000;

    @Inject

    public ElasticSelftest(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    @SneakyThrows
    public void helsesjekk() {
        long antallDokumenter = ElasticUtils.getCount();
        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i elastic (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            throw new RuntimeException(feilmelding);
        }


    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "elasticsearch helsesjekk",
                String.format("http://%s/%s", getElasticHostname(), getAlias()),
                String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER), true
        );
    }

}
