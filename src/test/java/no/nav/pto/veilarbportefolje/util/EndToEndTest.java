package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.IndexName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApplicationConfigTest.class)
public abstract class EndToEndTest {

    @Autowired
    protected ElasticTestClient elasticTestClient;

    @Autowired
    protected TestDataClient testDataClient;

    @Autowired
    protected ElasticIndexer elasticIndexer;

    @Autowired
    protected IndexName indexName;

    @BeforeEach
    void setUp() {
        try {
            elasticIndexer.opprettNyIndeks(indexName.getValue());
        } catch (Exception e) {
            elasticTestClient.deleteIndex(indexName);
            elasticIndexer.opprettNyIndeks(indexName.getValue());
        }
    }

    @AfterEach
    void tearDown() {
        elasticTestClient.deleteIndex(indexName);
    }

}
