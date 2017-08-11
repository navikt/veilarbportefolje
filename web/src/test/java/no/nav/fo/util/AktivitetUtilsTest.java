package no.nav.fo.util;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Aktivitet.AktivitetDTO;
import no.nav.fo.domene.Aktivitet.AktivitetData;
import no.nav.fo.domene.Aktivitet.AktivitetFullfortStatuser;
import org.apache.solr.common.SolrInputDocument;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.Arrays;

import static java.util.Arrays.asList;
import static no.nav.fo.util.AktivitetUtils.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktivitetUtilsTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Test
    public void aktivitetErAktiv() {
        String ikkeFullfortStatus1 = "detteErIkkeEnFullfortAktivitet1";
        String ikkeFullfortStatus2 = "detteErIkkeEnFullfortAktivitet2";
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus1)).isFalse();
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus2)).isFalse();

        assertThat(erBrukersAktivitetAktiv(asList(ikkeFullfortStatus1, ikkeFullfortStatus2))).isTrue();
    }

    @Test
    public void aktivitetErIkkeAktiv() {
        String fullfortStatus = AktivitetData.fullførteStatuser.get(0).toString();

        assertThat(erBrukersAktivitetAktiv(asList(fullfortStatus))).isFalse();
    }

    @Test
    public void aktivitetErIPeriode() {
        AktivitetDTO aktivitet = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-08T01:00:00+02:00"));

        LocalDate today1 = LocalDate.parse("2017-06-03");
        LocalDate today2 = LocalDate.parse("2017-06-01");
        LocalDate today3 = LocalDate.parse("2017-06-08");

        assertThat(erAktivitetIPeriode(aktivitet, today1)).isTrue();
        assertThat(erAktivitetIPeriode(aktivitet, today2)).isTrue();
        assertThat(erAktivitetIPeriode(aktivitet, today3)).isTrue();
    }

    @Test
    public void aktivitetErIkkeIperiode() {
        AktivitetDTO aktivitet = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-08T01:00:00+02:00"));

        LocalDate today1 = LocalDate.parse("2017-06-09");
        LocalDate today2 = LocalDate.parse("2017-05-31");
        LocalDate today3 = LocalDate.parse("2016-06-08");
        LocalDate today4 = LocalDate.parse("2018-06-08");

        assertThat(erAktivitetIPeriode(aktivitet, today1)).isFalse();
        assertThat(erAktivitetIPeriode(aktivitet, today2)).isFalse();
        assertThat(erAktivitetIPeriode(aktivitet, today3)).isFalse();
        assertThat(erAktivitetIPeriode(aktivitet, today4)).isFalse();
    }

    @Test
    public void brukerErIAktivAktivitet() {
        String fullfortStatus = AktivitetData.fullførteStatuser.get(0).toString();
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-06-03");

        AktivitetDTO aktivitet1 = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-08T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO aktivitet2 = new AktivitetDTO().setStatus(fullfortStatus);

        assertThat(erBrukerIAktivAktivitet(asList(aktivitet1,aktivitet2),today)).isTrue();
    }

    @Test
    public void brukerErIkkeIAktivAktivitet() {
        String fullfortStatus = AktivitetData.fullførteStatuser.get(0).toString();
        LocalDate today = LocalDate.parse("2017-06-03");

        AktivitetDTO aktivitet1 = new AktivitetDTO().setStatus(fullfortStatus);

        AktivitetDTO aktivitet2 = new AktivitetDTO().setStatus(fullfortStatus);

        assertThat(erBrukerIAktivAktivitet(asList(aktivitet1,aktivitet2),today)).isFalse();
    }

    @Test
    public void skalFinneNyesteUtlopteAktivteAktivitet() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-07-01");

        AktivitetDTO denEldsteAktiviteten = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-05-31T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);
        assertThat(nyesteIkkeFullforte).isEqualTo(denNyesteAktiviteten);
    }

    @Test
    public void skalReturnereNullNaarDetIkkeFinnesNoenUtlopteAktiviteter() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-05-01");

        AktivitetDTO denEldsteAktiviteten = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
                .setFraDato(DateUtils.timestampFromISO8601("2017-05-31T01:00:00+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);

        assertThat(nyesteIkkeFullforte).isNull();
    }

    @Test
    public void skalLeggeTilTiltakPaSolrDokument() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("person_id", "123");
        when(brukerRepository.getTiltak(anyString())).thenReturn(Arrays.asList("Tiltak1", "Tiltak2"));

        applyTiltak(Arrays.asList(solrInputDocument), brukerRepository);

        assertThat(solrInputDocument.keySet()).containsExactly("person_id", "tiltak");
        assertThat(solrInputDocument.getFieldValues("tiltak")).containsExactly("Tiltak1", "Tiltak2");
    }

    @Test
    public void skalIkkeLeggeTilTiltakPaSolrDokumentDersomTiltakIkkeFinnesForBrukeren() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("person_id", "12345678910");
        when(brukerRepository.getTiltak(anyString())).thenReturn(Lists.emptyList());

        applyTiltak(Arrays.asList(solrInputDocument), brukerRepository);

        assertThat(solrInputDocument.keySet()).containsExactly("person_id");
    }


}