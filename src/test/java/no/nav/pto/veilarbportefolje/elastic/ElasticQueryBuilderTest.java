package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.utils.Pair;
import no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.pto.veilarbportefolje.elastic.ElasticQueryBuilder.*;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.listOf;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.mapOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;

public class ElasticQueryBuilderTest {

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("FASIT_ENVIRONMENT_NAME", "test");
    }


    @Test
    public void skal_sortere_etternavn_paa_etternavn_feltet() {
        val searchSourceBuilder = sorterQueryParametere("asc", "etternavn", new SearchSourceBuilder(), new Filtervalg());
        val fieldName = searchSourceBuilder.sorts().get(0).toString();
        assertThat(fieldName).contains("etternavn");
    }

    @Test
    public void skal_bygge_riktig_filtrer_paa_veileder_script() {
        String actualScript = byggVeilederPaaEnhetScript(listOf("Z000000", "Z000001", "Z000002"));
        String expectedScript = "(doc.veileder_id.size() != 0 && [\"Z000000\",\"Z000001\",\"Z000002\"].contains(doc.veileder_id.value)).toString()";

        assertThat(actualScript).isEqualTo(expectedScript);
    }

    @Test
    public void skal_sortere_paa_aktiviteter_som_er_satt_til_ja() {
        val navnPaAktivitet = "behandling";
        val filtervalg = new Filtervalg().setAktiviteter(
                mapOf(
                        Pair.of(navnPaAktivitet, JA),
                        Pair.of("egen", NEI)
                )
        );

        val sorteringer = sorterValgteAktiviteter(filtervalg, new SearchSourceBuilder(), ASC);

        val expectedJson = readFileAsJsonString("/sorter_aktivitet_behandling.json");
        val actualJson = sorteringer.sorts().get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_nei_på_tiltak() {
        val filtervalg = new Filtervalg().setAktiviteter(mapOf(Pair.of("tiltak", NEI)));
        val builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        val expectedJson = readFileAsJsonString("/nei_paa_tiltak.json");
        val actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_ja_paa_behandling() {
        val filtervalg = new Filtervalg().setAktiviteter(mapOf(Pair.of("behandling", JA)));
        val builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        val expectedJson = readFileAsJsonString("/ja_paa_behandling.json");
        val actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_ja_paa_tiltak() {
        val filtervalg = new Filtervalg().setAktiviteter(mapOf(Pair.of("tiltak", AktivitetFiltervalg.JA)));
        val builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        val expectedJson = readFileAsJsonString("/ja_paa_tiltak.json");
        val actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_brukere_som_er_19_aar_og_under() {
        val builder = new BoolQueryBuilder();
        byggAlderQuery("19-og-under", builder);

        val actualJson = builder.toString();
        val expectedJson = readFileAsJsonString("/19_aar_og_under.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_brukere_mellom_20_24_aar() {
        val builder = new BoolQueryBuilder();
        byggAlderQuery("20-24", builder);

        val actualJson = builder.toString();
        val expectedJson = readFileAsJsonString("/mellom_20_24_aar.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_portefoljestorrelser() {
        val request = byggPortefoljestorrelserQuery("0000");

        val actualJson = request.aggregations().toString();
        val expectedJson = readFileAsJsonString("/portefoljestorrelser.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @SneakyThrows
    private String readFileAsJsonString(String pathname) {
        val URI = getClass().getResource(pathname).toURI();
        val encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }

}
