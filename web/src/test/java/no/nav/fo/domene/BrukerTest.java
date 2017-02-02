package no.nav.fo.domene;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class BrukerTest {
    @Test
    public void bruker_har_alle_obligatoriske_felt() throws Exception {
        SolrDocument document = new SolrDocument();
        document.addField("fodselsnr", "99999999999");
        document.addField("fornavn", "Rudolf");
        document.addField("etternavn", "Blodstrupmoen");
        document.addField("veileder_id", "XXXXXX");
        document.addField("sperret_ansatt", "false");
        document.addField("sikkerhetstiltak_type_kode", "foo");

        Bruker bruker = Bruker.of(document);
        assertThat(bruker.getFodselsnr(), notNullValue());
        assertThat(bruker.getFornavn(), notNullValue());
        assertThat(bruker.getEtternavn(), notNullValue());
        assertThat(bruker.getVeilderId(), notNullValue());
        assertThat(bruker.getSperretAnsatt(), notNullValue());
        assertThat(bruker.getSikkerhetstiltak(), notNullValue());
    }
}