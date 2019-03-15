package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.fo.veilarbportefolje.domene.Filtervalg;
import no.nav.fo.veilarbportefolje.util.Pair;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticQueryBuilder.*;
import static no.nav.fo.veilarbportefolje.util.CollectionUtils.listOf;
import static no.nav.fo.veilarbportefolje.util.CollectionUtils.mapOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;

public class ElasticQueryBuilderTest {

    @Rule
    public SystemPropertiesRule rule = new SystemPropertiesRule().setProperty("FASIT_ENVIRONMENT_NAME", "test");


    @Test
    public void skal_sortere_etternavn_paa_fullt_navn_feltet() {
        val searchSourceBuilder = sorterQueryParametere("asc", "etternavn", new SearchSourceBuilder(), new Filtervalg());
        val fieldName = searchSourceBuilder.sorts().get(0).toString();
        assertThat(fieldName).contains("fullt_navn.raw");
    }

    @Test
    public void skal_bygge_riktig_filtrer_paa_veileder_script() {
        String actualScript = byggVeilederPaaEnhetScript(listOf("Z000000", "Z000001", "Z000002"));
        String expectedScript = "[\"Z000000\",\"Z000001\",\"Z000002\"].contains(doc.veileder_id.value)";

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
    public void skal_bygge_korrekt_json_for_aa_hente_ut_brukere_med_arbeidsliste() {
        val request = byggArbeidslisteQuery("0000", "Z00000");

        val actualJson = request.query().toString();
        val expectedJson = readFileAsJsonString("/brukere_med_arbeidslist.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_portefoljestorrelser() {
        val request = byggPortefoljestorrelserQuery("0000");

        val actualJson = request.aggregations().toString();
        val expectedJson = readFileAsJsonString("/portefoljestorrelser.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_statustall_for_veileder() {
        val builder = byggStatusTallForVeilederQuery("0000", "Z000000", listOf("Z00001"));

        val actualJson = builder.aggregations().toString();
        val expectedJson = readFileAsJsonString("/statustall_for_veileder.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_statustall_for_enhet() {
        val builder = byggStatusTallForEnhetQuery("0000", listOf("Z00001"));

        val actualJson = builder.aggregations().toString();
        val expectedJson = readFileAsJsonString("/statustall_for_enhet.json");

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @SneakyThrows
    private String readFileAsJsonString(String pathname) {
        val URI = getClass().getResource(pathname).toURI();
        val encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }

}
