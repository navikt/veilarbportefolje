package no.nav.pto.veilarbportefolje.aktiviteter;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetData.aktivitetTyperFraAktivitetsplanList;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetData.aktivitetTyperFraKafka;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.erAktivitetIPeriode;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.finnNyesteUtlopteAktivAktivitet;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.konverterTilBrukerOppdatering;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.lagAktivitetSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktivitetUtilsTest {

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private BrukerService brukerService;

    @Test
    public void konverterTilBrukerOppdatering_skal_hente_rett_brukerOppdateringer() throws Exception {

        var TODAY = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        var TOMORROW = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
        var DAY_AFTER_TOMORROW = Timestamp.valueOf(LocalDate.now().plusDays(2).atStartOfDay());

        var YESTERDAY = Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay());
        var DAY_BEFORE_YESTERDAY = Timestamp.valueOf(LocalDate.now().minusDays(2).atStartOfDay());


        var aktivitetDTO1 = new AktivitetDTO()
                .setTilDato(TOMORROW)
                .setFraDato(TODAY);

        var aktivitetDTO2 = new AktivitetDTO()
                .setTilDato(YESTERDAY)
                .setFraDato(DAY_BEFORE_YESTERDAY);

        var aktivitetDTO3 = new AktivitetDTO()
                .setTilDato(TOMORROW)
                .setFraDato(YESTERDAY);

        var aktivitetDTO4 = new AktivitetDTO()
                .setTilDato(DAY_AFTER_TOMORROW)
                .setFraDato(DAY_AFTER_TOMORROW);

        var aktivitetDTO5 = new AktivitetDTO()
                .setTilDato(DAY_AFTER_TOMORROW)
                .setFraDato(Timestamp.valueOf(LocalDate.now().atStartOfDay().plusHours(1)));


        var aktiviteter = Arrays.asList(aktivitetDTO1, aktivitetDTO2, aktivitetDTO3, aktivitetDTO4, aktivitetDTO5);
        var aktorAktiviteter = new AktoerAktiviteter("123").setAktiviteter(aktiviteter);

        when(brukerService.hentPersonidFraAktoerid(any())).thenReturn(Try.of(() -> PersonId.of("123")));
        var brukerOppdateringer = konverterTilBrukerOppdatering(aktorAktiviteter, brukerService);

        assertThat(brukerOppdateringer.getNyesteUtlopteAktivitet()).isEqualTo(YESTERDAY);
        assertThat(brukerOppdateringer.getAktivitetStart()).isEqualTo(TODAY);
        assertThat(brukerOppdateringer.getNesteAktivitetStart()).isEqualTo(DAY_AFTER_TOMORROW);
        assertThat(brukerOppdateringer.getForrigeAktivitetStart()).isEqualTo(YESTERDAY);
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
    public void skalFinneNyesteUtlopteAktivteAktivitet() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetIkkeAktivStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-07-01");

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denEldsteAktiviteten = new AktivitetDTO()
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);
        assertThat(nyesteIkkeFullforte).isEqualTo(denNyesteAktiviteten);
    }

    @Test
    public void skalReturnereNullNaarDetIkkeFinnesNoenUtlopteAktiviteter() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetIkkeAktivStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-05-01");

        AktivitetDTO denNyesteAktiviteten = new AktivitetDTO()
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-02T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO denEldsteAktiviteten = new AktivitetDTO()
                .setTilDato(DateUtils.timestampFromISO8601("2017-06-01T01:00:00+02:00"))
                .setStatus(ikkeFullfortStatus);

        AktivitetDTO nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);

        assertThat(nyesteIkkeFullforte).isNull();
    }

    @Test
    public void skalReturnereSetMedAlleAktivitetstyper() {
        Set<AktivitetStatus> statuser = lagAktivitetSet(Collections.emptyList(), LocalDate.now(), AktorId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.size()).isEqualTo(aktivitetTyperFraKafka.size());
        statuser.forEach((status) -> {
            assertThat(status.isAktiv()).isFalse();
            assertThat(status.getNesteUtlop()).isNull();
        });
    }

    @Test
    public void skalSortereNyesteUtlopsdatoForst() {
        String aktivitetstype = aktivitetTyperFraAktivitetsplanList.get(0).toString();
        String IKKE_FULLFORT_STATUS = "IKKE_FULLFORT_STATUS";
        Timestamp t1 = new Timestamp(100000000);
        Timestamp t2 = new Timestamp(200000000);
        AktivitetDTO a1 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t1);
        AktivitetDTO a2 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t2);

        Set<AktivitetStatus> statuser = lagAktivitetSet(asList(a1, a2), LocalDate.ofEpochDay(0), AktorId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.stream().filter((a) -> a.getAktivitetType().equals(aktivitetstype)).findFirst().get().getNesteUtlop()).isEqualTo(t1);

    }

    @Test
    public void skalFinneUtlopsdatoNaarEnerNull() {
        String aktivitetstype = AktivitetsType.values()[0].toString();
        String IKKE_FULLFORT_STATUS = "IKKE_FULLFORT_STATUS";
        Timestamp t1 = new Timestamp(200000000);
        AktivitetDTO a1 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS);
        AktivitetDTO a2 = new AktivitetDTO().setAktivitetType(aktivitetstype).setStatus(IKKE_FULLFORT_STATUS).setTilDato(t1);

        Set<AktivitetStatus> statuser = lagAktivitetSet(asList(a1, a2), LocalDate.ofEpochDay(0), AktorId.of("aktoerid"), PersonId.of("personid"));
        assertThat(statuser.stream().filter((a) -> a.getAktivitetType().equals(aktivitetstype)).findFirst().get().getNesteUtlop()).isEqualTo(t1);
    }
}
