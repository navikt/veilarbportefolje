package no.nav.pto.veilarbportefolje.opensearch;

import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt;
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchQueryBuilder.*;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.search.sort.SortOrder.ASC;

public class OpensearchQueryBuilderTest {

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("FASIT_ENVIRONMENT_NAME", "test");
    }


    @Test
    public void skal_sortere_etternavn_paa_etternavn_feltet() {
        var searchSourceBuilder = sorterQueryParametere(Sorteringsrekkefolge.STIGENDE, Sorteringsfelt.ETTERNAVN, new SearchSourceBuilder(), new Filtervalg(), new BrukerinnsynTilganger(true, true, true));
        var fieldName = searchSourceBuilder.sorts().get(0).toString();
        assertThat(fieldName).contains(Sorteringsfelt.ETTERNAVN.sorteringsverdi);
    }

    @Test
    public void skal_bygge_riktig_filtrer_paa_veileder_script() {
        String actualScript = byggVeilederPaaEnhetScript(List.of("Z000000", "Z000001", "Z000002"));
        String expectedScript = "(doc.veileder_id.size() != 0 && [\"Z000000\",\"Z000001\",\"Z000002\"].contains(doc.veileder_id.value)).toString()";

        assertThat(actualScript).isEqualTo(expectedScript);
    }

    @Test
    public void skal_sortere_paa_aktiviteter_som_er_satt_til_ja() {
        var navnPaAktivitet = "behandling";
        var filtervalg = new Filtervalg().setAktiviteter(
                Map.of(
                        navnPaAktivitet, JA,
                        "egen", NEI
                )
        );

        var sorteringer = sorterValgteAktiviteter(filtervalg, new SearchSourceBuilder(), ASC);

        var expectedJson = readFileAsJsonString("/sorter_aktivitet_behandling.json", getClass());
        var actualJson = sorteringer.sorts().get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_nei_på_tiltak() {
        var filtervalg = new Filtervalg().setAktiviteter(Map.of("tiltak", NEI));
        var builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        var expectedJson = readFileAsJsonString("/nei_paa_tiltak.json", getClass());
        var actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_ja_paa_behandling() {
        var filtervalg = new Filtervalg().setAktiviteter(Map.of("behandling", JA));
        var builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        var expectedJson = readFileAsJsonString("/ja_paa_behandling.json", getClass());
        var actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_om_man_velger_ja_paa_tiltak() {
        var filtervalg = new Filtervalg().setAktiviteter(Map.of("tiltak", AktivitetFiltervalg.JA));
        var builders = byggAktivitetFilterQuery(filtervalg, boolQuery());

        var expectedJson = readFileAsJsonString("/ja_paa_tiltak.json", getClass());
        var actualJson = builders.get(0).toString();

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_brukere_som_er_19_aar_og_under() {
        var builder = new BoolQueryBuilder();
        byggAlderQuery("19-og-under", builder);

        var actualJson = builder.toString();
        var expectedJson = readFileAsJsonString("/19_aar_og_under.json", getClass());

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_brukere_mellom_20_24_aar() {
        var builder = new BoolQueryBuilder();
        byggAlderQuery("20-24", builder);

        var actualJson = builder.toString();
        var expectedJson = readFileAsJsonString("/mellom_20_24_aar.json", getClass());

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void skal_bygge_korrekt_json_for_aa_hente_ut_portefoljestorrelser() {
        var request = byggPortefoljestorrelserQuery("0000");

        var actualJson = request.aggregations().toString();
        var expectedJson = readFileAsJsonString("/portefoljestorrelser.json", getClass());

        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

}
