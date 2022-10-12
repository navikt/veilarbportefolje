package no.nav.pto.veilarbportefolje.config;

import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.OpenSearchClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class OpenSearchContainer extends GenericContainer<OpenSearchContainer>{

    public OpenSearchContainer(String opensearchVersion) {
        super(DockerImageName.parse("opensearchproject/opensearch")
                .withTag(opensearchVersion));
    }

    /**
     * Denne versjonen bør samsvare med versjon av instans i Aiven. Versjon kan sjekkes ved å spørre i
     * <a href="https://nav-it.slack.com/archives/C5KUST8N6/p1665564633449399">#nais</a>.
     */
    public OpenSearchContainer() {
        super(DockerImageName.parse("opensearchproject/opensearch")
                .withTag("1.2.4"));
        log.info("Starter opensearch med versjon: {}", OpenSearchClient.class.getPackage().getImplementationVersion());
    }

    @Override
    public void start(){
        withExposedPorts(9200);
        withEnv("discovery.type", "single-node");
        withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true");
        withEnv("DISABLE_SECURITY_PLUGIN", "true");

        super.start();
    }
}
