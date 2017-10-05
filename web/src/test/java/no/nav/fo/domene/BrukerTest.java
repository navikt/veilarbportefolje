package no.nav.fo.domene;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static no.nav.fo.util.DateUtils.toLocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class BrukerTest {
    @Test
    public void skalFylleUtAlleObligatoriskeFeltPaaBruker() throws Exception {
        Bruker bruker = Bruker.of(createSolrDocument("6"));

        assertThat(bruker.getFnr(), notNullValue());
        assertThat(bruker.getFornavn(), notNullValue());
        assertThat(bruker.getEtternavn(), notNullValue());
        assertThat(bruker.getVeilederId(), notNullValue());
        assertThat(bruker.isEgenAnsatt(), notNullValue());
        assertThat(bruker.getSikkerhetstiltak(), notNullValue());
        assertThat(bruker.getDiskresjonskode(), notNullValue());
        assertThat(bruker.isErDoed(), notNullValue());
    }

    @Test
    public void skalFiltrereBortDiskresjonskode() throws Exception {
        Bruker brukerKode5 = Bruker.of(createSolrDocument("5"));
        Bruker brukerKode6 = Bruker.of(createSolrDocument("6"));
        Bruker brukerKode7 = Bruker.of(createSolrDocument("7"));

        assertThat(brukerKode5.getDiskresjonskode(), nullValue());
        assertThat(brukerKode6.getDiskresjonskode()).isEqualTo("6");
        assertThat(brukerKode7.getDiskresjonskode()).isEqualTo("7");
    }

    @Test
    public void parseDato() throws Exception {
        Date date = Date.from(LocalDate.of(2017, Month.MARCH, 30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        LocalDateTime dato = toLocalDateTime(date);

        assertThat(dato.getYear()).isEqualTo(2017);
        assertThat(dato.getMonth()).isEqualTo(Month.MARCH);
        assertThat(dato.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    public void skalLeggeTilAlleAktiviteter() {
        SolrDocument solrDocument = createSolrDocument(null);
        solrDocument.addField("aktiviteter", "aktivitet1");
        solrDocument.addField("aktiviteter", "aktivitet2");
        Bruker bruker = Bruker.of(solrDocument);
        Map<String, Timestamp> map = bruker.getAktiviteter();
        assertThat(map).containsOnlyKeys("aktivitet1", "aktivitet2");
        assertThat(map.get("aktivitet1")).isNull();
        assertThat(map.get("aktivitet2")).isNull();
    }

    @Test
    public void skalLeggeTilDatoPaAktiviteter() {
        SolrDocument solrDocument = createSolrDocument(null);
        solrDocument.addField("aktiviteter", "aktivitet1");
        solrDocument.addField("aktiviteter", "aktivitet2");
        solrDocument.addField("aktiviteter_utlopsdato_json", "{\"aktivitet1\":\"1970-01-01T01:01:01Z\"}");
        Bruker bruker = Bruker.of(solrDocument);
        Map<String, Timestamp> map = bruker.getAktiviteter();
        assertThat(map).containsOnlyKeys("aktivitet1", "aktivitet2");
        assertThat(map.get("aktivitet1")).isNotNull();
        assertThat(map.get("aktivitet2")).isNull();
    }

    @Test
    public void skalIkkeTryneOmAktiviteterErNull() {
        SolrDocument solrDocument = createSolrDocument(null);
        Bruker bruker = Bruker.of(solrDocument);
        Map<String, Timestamp> map = bruker.getAktiviteter();
    }

    @Test
    public void skalReturnereNesteAktivitetUtlopsdato() {
        Timestamp t1 = new Timestamp(1000000000);
        Timestamp t2 = new Timestamp(2000000000);
        Map<String, Timestamp> map = new HashMap<>();
        map.put("a1", t1 );
        map.put("a2", t2 );

        Bruker bruker =  new Bruker().setAktiviteter(map);
        assertThat(bruker.getNesteAktivitetUtlopsdatoOrElseEpoch0()).isEqualTo(t1);
    }

    @Test
    public void skalReturnereEpoch0OmIngenUtlopsdatoerForAktivteter() {
        Map<String, Timestamp> map = new HashMap<>();
        map.put("a1", null);
        Bruker bruker = new Bruker().setAktiviteter(map);
        assertThat(bruker.getNesteAktivitetUtlopsdatoOrElseEpoch0()).isEqualTo(new Timestamp(0));
    }

    @Test
    public void skalReturnereEpoch0OmAktivteterIkkeErDefiner() {
        Bruker bruker = new Bruker();
        assertThat(bruker.getNesteAktivitetUtlopsdatoOrElseEpoch0()).isEqualTo(new Timestamp(0));
    }

    @Test
    public void skalReturnereUtlopsdatoForAktivitet() {
        Timestamp t1 = new Timestamp(1000000000);
        Map<String, Timestamp> map = new HashMap<>();
        map.put("a1", t1 );
        Bruker bruker = new Bruker().setAktiviteter(map);
        assertThat(bruker.getNesteUtlopsdatoForAktivitetOrElseEpoch0("a1")).isEqualTo(t1);
    }

    @Test
    public void skalReturnereEpoch0OmUtlopsdatoForAktivitetIkkeFinnes() {
        Timestamp t1 = new Timestamp(1000000000);
        Map<String, Timestamp> map = new HashMap<>();
        map.put("a1", t1 );
        Bruker bruker = new Bruker();
        assertThat(bruker.getNesteUtlopsdatoForAktivitetOrElseEpoch0("aktiviteter_finnesikke")).isEqualTo(new Timestamp(0));
    }

    private SolrDocument createSolrDocument(String kode) {
        SolrDocument document = new SolrDocument();
        document.addField("fnr", "99999999999");
        document.addField("fornavn", "Rudolf");
        document.addField("etternavn", "Blodstrupmoen");
        document.addField("veileder_id", "XXXXXX");
        document.addField("utlopsdato", new Date());
        document.addField("egen_ansatt", false);
        document.addField("sikkerhetstiltak", "foo");
        document.addField("er_doed", false);
        document.addField("diskresjonskode", kode);
        document.addField("fodselsdag_i_mnd", 99);
        document.addField("fodselsdato", new Date());
        document.addField("kjonn", "K");
        document.addField("erInaktiv", false);
        document.addField("arbeidsliste_aktiv", true);
        document.addField("arbeidsliste_endringstidspunkt", new Date());
        document.addField("arbeidsliste_kommentar", "Dette er en kommentar");
        document.addField("arbeidsliste_frist", new Date());

        return document;
    }
}

