package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.*;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;

@Component
public class OpenDistroElasticSelftest implements Helsesjekk {

    OpenDistroClient client;

    private static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;

    @Inject
    public OpenDistroElasticSelftest(OpenDistroClient client) {
        this.client = client;
    }

    @Override
    @SneakyThrows
    public void helsesjekk() {
        long antallDokumenter = ElasticUtils.getCount(VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME, VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME, VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD);
        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i elastic (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            throw new RuntimeException(feilmelding);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "elasticsearch helsesjekk",
                String.format("http://%s/%s", VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME, getAlias()),
                String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER), true
        );
    }

}
