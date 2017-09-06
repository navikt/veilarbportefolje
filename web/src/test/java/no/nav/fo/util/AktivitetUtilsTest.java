package no.nav.fo.util;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetData;
import no.nav.fo.domene.aktivitet.AktivitetFullfortStatuser;
import org.apache.solr.common.SolrInputDocument;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static no.nav.fo.domene.aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.AktivitetUtils.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
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
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-08T01:00:00Z"));

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
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-08T01:00:00Z"));

        LocalDate today1 = LocalDate.parse("2017-06-09");
        LocalDate today3 = LocalDate.parse("2017-06-10");
        LocalDate today4 = LocalDate.parse("2018-06-08");

        assertThat(erAktivitetIPeriode(aktivitet, today1)).isFalse();
        assertThat(erAktivitetIPeriode(aktivitet, today3)).isFalse();
        assertThat(erAktivitetIPeriode(aktivitet, today4)).isFalse();
    }

    @Test
    public void aktivitetMedNullTilDatoErAktiv() {
        AktivitetDTO aktivitet = new AktivitetDTO();

        LocalDate today1 = LocalDate.parse("2017-06-09");

        assertThat(erAktivitetIPeriode(aktivitet, today1)).isTrue();
    }

    @Test
    public void brukerErIAktivAktivitet() {
        String fullfortStatus = AktivitetData.fullførteStatuser.get(0).toString();
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetFullfortStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-06-03");

        AktivitetDTO aktivitet1 = new AktivitetDTO()
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
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
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
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);

        assertThat(nyesteIkkeFullforte).isNull();
    }

    @Test
    public void skalLeggeTilAktiviteterPaSolrDokument() {
        PersonId personId = new PersonId("persondid");
        AktoerId aktoerId = new AktoerId("aktoerid");
        Timestamp nyesteUtlop = new Timestamp(0);


        AktivitetStatus a1 = AktivitetStatus.of(personId, aktoerId, "aktivitetstype1",true, nyesteUtlop);
        AktivitetStatus a2 = AktivitetStatus.of(personId, aktoerId, "aktivitetstype2",false, nyesteUtlop);

        Set<AktivitetStatus> aktivitetStatuses = new HashSet<>();
        aktivitetStatuses.add(a1);
        aktivitetStatuses.add(a2);

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("person_id", personId.toString());

        Map<PersonId, Set<AktivitetStatus>> returnMap = new HashMap<>();
        returnMap.put(personId, aktivitetStatuses);

        when(brukerRepository.getAktivitetstatusForBrukere(any())).thenReturn(returnMap);

        applyAktivitetStatuser(solrInputDocument, brukerRepository);


        assertThat((ArrayList) solrInputDocument.get("aktiviteter").getValue()).contains("aktivitetstype1");
        assertThat((ArrayList) solrInputDocument.get("aktiviteter").getValue()).doesNotContain("aktivitetstype2");
        assertThat((String) solrInputDocument.get("aktiviteter_utlopsdato_json").getValue()).contains("aktivitetstype1");
        assertThat((String) solrInputDocument.get("aktiviteter_utlopsdato_json").getValue()).doesNotContain("aktivitetstype2");
        assertThat((String) solrInputDocument.get("aktiviteter_utlopsdato_json").getValue()).contains(DateUtils.iso8601FromTimestamp(nyesteUtlop));
    }

    @Test
    public void skalIkkeLeggeTilAktiviteter() {
        PersonId personId = new PersonId("persondid");
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("person_id", personId.toString());

        when(brukerRepository.getAktivitetstatusForBrukere(any())).thenReturn(new HashMap<>());
        applyAktivitetStatuser(solrInputDocument, brukerRepository);
        assertThat(solrInputDocument.get("aktiviteter")).isNull();
        assertThat(solrInputDocument.get("aktiviteter_utlopsdato_json")).isNull();
    }

    @Test
    public void skalReturnereSetMedAlleAktivitetstyper() {
        Set<AktivitetStatus> statuser = lagAktivitetSet(Collections.emptyList(), LocalDate.now(), AktoerId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.size()).isEqualTo(aktivitetTyperList.size());
        statuser.forEach((status) -> {
            assertThat(status.isAktiv()).isFalse();
            assertThat(status.getNesteUtlop()).isNull();
        });
    }

    @Test
    public void skalSortereNyesteUtlopsdatoForst() {
        String aktivitetstype = aktivitetTyperList.get(0).toString();
        String IKKE_FULLFORT_STATUS = "IKKE_FULLFORT_STATUS";
        Timestamp t1 = new Timestamp(100000000);
        Timestamp t2 = new Timestamp(200000000);
        AktivitetDTO a1 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t1);
        AktivitetDTO a2 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t2);

        Set<AktivitetStatus> statuser = lagAktivitetSet(asList(a1,a2),LocalDate.ofEpochDay(0), AktoerId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.stream().filter((a) -> a.getAktivitetType().equals(aktivitetstype)).findFirst().get().getNesteUtlop()).isEqualTo(t1);

    }

    @Test
    public void skalFinneUtlopsdatoNaarÉnerNull() {
        String aktivitetstype = aktivitetTyperList.get(0).toString();
        String IKKE_FULLFORT_STATUS = "IKKE_FULLFORT_STATUS";
        Timestamp t1 = new Timestamp(200000000);
        AktivitetDTO a1 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS);
        AktivitetDTO a2 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t1);

        Set<AktivitetStatus> statuser = lagAktivitetSet(asList(a1,a2),LocalDate.ofEpochDay(0), AktoerId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.stream().filter((a) -> a.getAktivitetType().equals(aktivitetstype)).findFirst().get().getNesteUtlop()).isEqualTo(t1);
    }

    @Test
    public void skalLeggeTilTiltakPaSolrDokument() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        Fnr fnr = Fnr.of("11111111111");
        solrInputDocument.addField("fnr", fnr.toString());

        Map<Fnr, Set<Brukertiltak>> tiltak = new HashMap<>();
        Set<Brukertiltak> brukertiltakSet = new HashSet<>();

        brukertiltakSet.add(Brukertiltak.of(fnr,"Tiltak1"));
        brukertiltakSet.add(Brukertiltak.of(fnr,"Tiltak2"));
        tiltak.put(fnr, brukertiltakSet);

        when(brukerRepository.getBrukertiltak(anyList())).thenReturn(tiltak);

        applyTiltak(Arrays.asList(solrInputDocument), brukerRepository);

        assertThat(solrInputDocument.keySet()).containsExactly("fnr", "tiltak");
        assertThat(solrInputDocument.getFieldValues("tiltak")).containsExactly("Tiltak1", "Tiltak2");
    }

    @Test
    public void skalIkkeLeggeTilTiltakPaSolrDokumentDersomTiltakIkkeFinnesForBrukeren() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("fnr", "12345678910");
        when(brukerRepository.getBrukertiltak(anyString())).thenReturn(Lists.emptyList());

        applyTiltak(Arrays.asList(solrInputDocument), brukerRepository);

        assertThat(solrInputDocument.keySet()).containsExactly("fnr");
    }


}