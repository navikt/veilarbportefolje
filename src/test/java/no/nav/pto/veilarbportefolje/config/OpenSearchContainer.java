package no.nav.pto.veilarbportefolje.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class OpenSearchContainer extends GenericContainer<OpenSearchContainer>{


    public OpenSearchContainer(String opensearchVersion) {
        super(DockerImageName.parse("opensearchproject/opensearch")
                .withTag(opensearchVersion));
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
