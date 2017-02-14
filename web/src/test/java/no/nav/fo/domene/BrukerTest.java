package no.nav.fo.domene;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class BrukerTest {
    @Test
    public void skalFylleUtAlleObligatoriskeFeltPaaBruker() throws Exception {
        SolrDocument document = new SolrDocument();
        document.addField("fnr", "99999999999");
        document.addField("fornavn", "Rudolf");
        document.addField("etternavn", "Blodstrupmoen");
        document.addField("veileder_id", "XXXXXX");
        document.addField("egen_ansatt", false);
        document.addField("sikkerhetstiltak", "foo");
        document.addField("er_doed", false);
        document.addField("diskresjonskode", "6");

        Bruker bruker = Bruker.of(document);
        assertThat(bruker.getFnr(), notNullValue());
        assertThat(bruker.getFornavn(), notNullValue());
        assertThat(bruker.getEtternavn(), notNullValue());
        assertThat(bruker.getVeilederId(), notNullValue());
        assertThat(bruker.getEgenAnsatt(), notNullValue());
        assertThat(bruker.getSikkerhetstiltak(), notNullValue());
        assertThat(bruker.getDiskresjonskode(), notNullValue());
        assertThat(bruker.getErDoed(), notNullValue());
    }
}