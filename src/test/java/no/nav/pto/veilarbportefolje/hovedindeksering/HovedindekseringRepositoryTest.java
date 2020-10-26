package no.nav.pto.veilarbportefolje.hovedindeksering;

import com.google.common.base.Joiner;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.HovedindekseringRepository;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.domene.AAPMaxtidUkeFasettMapping.UKE_UNDER12;
import static no.nav.pto.veilarbportefolje.domene.DagpengerUkeFasettMapping.UKE_UNDER2;
import static org.assertj.core.api.Assertions.assertThat;

public class HovedindekseringRepositoryTest {

    private static JdbcTemplate jdbcTemplate;
    private static HovedindekseringRepository hovedindekseringRepository;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();
        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);

        jdbcTemplate = new JdbcTemplate(ds);
        hovedindekseringRepository = new HovedindekseringRepository(jdbcTemplate, namedParameterJdbcTemplate);
    }

    @Before
    public void setUp() {
        try {
            List<String> lines = IOUtils.readLines(HovedindekseringRepositoryTest.class.getResourceAsStream("/insert-test-data-oppfolgingsbruker.sql"), UTF_8);
            this.jdbcTemplate.execute(Joiner.on("\n").join(lines));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table bruker_data");
    }

    @Test
    public void skal_oppdatere_om_bruker_finnes() {
        String personId = "personid";

        Brukerdata brukerdata1 = brukerdata(
                "aktoerid",
                personId,
                "veilederid",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER12,
                2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                true,
                true
        );

        Brukerdata brukerdata2 = brukerdata(
                "aktoerid",
                personId,
                "veilederid2",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER2,
                0,
                UKE_UNDER12,
                2,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                false,
                true
        );

        hovedindekseringRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), emptyList());
        hovedindekseringRepository.insertOrUpdateBrukerdata(singletonList(brukerdata1), singletonList(personId));

        Brukerdata brukerdataAfterInsert = hovedindekseringRepository.retrieveBrukerdata(List.of(personId)).get(0);

        assertThatBrukerdataIsEqual(brukerdata1, brukerdataAfterInsert);

        hovedindekseringRepository.insertOrUpdateBrukerdata(singletonList(brukerdata2), emptyList());
        Brukerdata brukerdataAfterUpdate = hovedindekseringRepository.retrieveBrukerdata(List.of(personId)).get(0);
        assertThatBrukerdataIsEqual(brukerdata2, brukerdataAfterUpdate);
    }


    @Test
    public void skal_inserte_om_bruker_ikke_finnes() {
        Brukerdata brukerdata = brukerdata(
                "aktoerid",
                "personid",
                "veielderid",
                Timestamp.from(Instant.now()),
                YtelseMapping.DAGPENGER_MED_PERMITTERING,
                LocalDateTime.now(),
                ManedFasettMapping.MND1,
                3,
                DagpengerUkeFasettMapping.UKE2_5,
                3,
                DagpengerUkeFasettMapping.UKE2_5,
                2,
                UKE_UNDER12,
                1,
                AAPUnntakUkerIgjenFasettMapping.UKE_UNDER12,
                false,
                true
        );

        hovedindekseringRepository.insertOrUpdateBrukerdata(singletonList(brukerdata), emptyList());

        Brukerdata brukerdataFromDb = hovedindekseringRepository.retrieveBrukerdata(List.of("personid")).get(0);

        assertThatBrukerdataIsEqual(brukerdata, brukerdataFromDb);
    }

    @Test
    public void retrieve_brukerdata_skal_inneholde_alle_felter() {

        Brukerdata brukerdata = new Brukerdata()
                .setNyesteUtlopteAktivitet(Timestamp.from(Instant.now()))
                .setPersonid("personid")
                .setAapmaxtidUke(1)
                .setAapmaxtidUkeFasett(AAPMaxtidUkeFasettMapping.UKE_UNDER12)
                .setAktoerid("aktoerid")
                .setUtlopsdato(LocalDateTime.now())
                .setUtlopsFasett(ManedFasettMapping.MND1)
                .setYtelse(YtelseMapping.AAP_MAXTID)
                .setAktivitetStart(new Timestamp(1))
                .setNesteAktivitetStart(new Timestamp(2))
                .setForrigeAktivitetStart(new Timestamp(3));

        hovedindekseringRepository.upsertBrukerdata(brukerdata);

        Brukerdata brukerdataFromDB = hovedindekseringRepository.retrieveBrukerdata(singletonList("personid")).get(0);

        assertThat(brukerdataFromDB).isEqualTo(brukerdata);
    }

    private Brukerdata brukerdata(
            String aktoerid,
            String personId,
            String veileder,
            Timestamp tildeltTidspunkt,
            YtelseMapping ytelse,
            LocalDateTime utlopsdato,
            ManedFasettMapping utlopsdatoFasett,
            Integer dagpUtlopUke,
            DagpengerUkeFasettMapping dagpUtlopUkeFasett,
            Integer permutlopUke,
            DagpengerUkeFasettMapping permutlopUkeFasett,
            Integer aapmaxtidUke,
            AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett,
            Integer aapUnntakDagerIgjen,
            AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasett,
            Boolean nyForVeileder,
            boolean oppfolging
    ) {
        return new Brukerdata()
                .setAktoerid(aktoerid)
                .setPersonid(personId)
                .setUtlopsdato(utlopsdato)
                .setUtlopsFasett(utlopsdatoFasett)
                .setDagputlopUke(dagpUtlopUke)
                .setDagputlopUkeFasett(dagpUtlopUkeFasett)
                .setPermutlopUke(permutlopUke)
                .setPermutlopUkeFasett(permutlopUkeFasett)
                .setAapmaxtidUke(aapmaxtidUke)
                .setAapmaxtidUkeFasett(aapmaxtidUkeFasett)
                .setAapUnntakDagerIgjen(aapUnntakDagerIgjen)
                .setAapunntakUkerIgjenFasett(aapUnntakUkerIgjenFasett)
                .setYtelse(ytelse);
    }

    private void assertThatBrukerdataIsEqual(Brukerdata b1, Brukerdata b2) {
        assertThat(b1.getPersonid()).isEqualTo(b2.getPersonid());
        assertThat(b1.getAktoerid()).isEqualTo(b2.getAktoerid());
        assertThat(b1.getUtlopsdato()).isEqualTo(b2.getUtlopsdato());
        assertThat(b1.getUtlopsFasett()).isEqualTo(b2.getUtlopsFasett());
        assertThat(b1.getDagputlopUke()).isEqualTo(b2.getDagputlopUke());
        assertThat(b1.getDagputlopUkeFasett()).isEqualTo(b2.getDagputlopUkeFasett());
        assertThat(b1.getPermutlopUke()).isEqualTo(b2.getPermutlopUke());
        assertThat(b1.getPermutlopUkeFasett()).isEqualTo(b2.getPermutlopUkeFasett());
        assertThat(b1.getAapmaxtidUke()).isEqualTo(b2.getAapmaxtidUke());
        assertThat(b1.getAapmaxtidUkeFasett()).isEqualTo(b2.getAapmaxtidUkeFasett());
        assertThat(b1.getAapUnntakDagerIgjen()).isEqualTo(b2.getAapUnntakDagerIgjen());
        assertThat(b1.getAapunntakUkerIgjenFasett()).isEqualTo(b2.getAapunntakUkerIgjenFasett());
        assertThat(b1.getYtelse()).isEqualTo(b2.getYtelse());
    }

}
