package no.nav.fo.database;

import com.google.common.collect.ImmutableSet;
import no.nav.fo.config.DatabaseFlywayConfigTest;
import no.nav.fo.domene.AktivitetData;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.util.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DatabaseFlywayConfigTest.class})
public class BrukerRepositoryFlywayTest {

    // TODO skrive om BrukerRepositoryTest slik at den også bruker flyway

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;


    @Test
    public void skalSetteInnAktivitet() throws Exception {
        AktivitetDataFraFeed aktivitet = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(DateUtils.timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setOpprettetDato(DateUtils.timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("STATUS");

        brukerRepository.upsertAktivitet(aktivitet);

        String status = (String) jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase("STATUS");
    }

    @Test
    public void skalOppdatereAktivitet() throws Exception {
        AktivitetDataFraFeed aktivitet1 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(DateUtils.timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setOpprettetDato(DateUtils.timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("IKKE STARTET");

        AktivitetDataFraFeed aktivitet2 = new AktivitetDataFraFeed()
                .setAktivitetId("aktivitetid")
                .setAktivitetType("aktivitettype")
                .setAktorId("aktoerid")
                .setAvtalt(false)
                .setFraDato(DateUtils.timestampFromISO8601("2017-03-03T10:10:10+02:00"))
                .setTilDato(DateUtils.timestampFromISO8601("2017-12-03T10:10:10+02:00"))
                .setOpprettetDato(DateUtils.timestampFromISO8601("2017-02-03T10:10:10+02:00"))
                .setStatus("FERDIG");

        brukerRepository.upsertAktivitet(aktivitet1);
        brukerRepository.upsertAktivitet(aktivitet2);

        String status = (String) jdbcTemplate.queryForList("select * from aktiviteter where aktivitetid='aktivitetid'").get(0).get("status");

        assertThat(status).isEqualToIgnoringCase("FERDIG");

    }

    @Test
    public void skalReturnereNullPaaAlleStatuserDersomBrukerIkkeFinnes() {
        Map<String, Timestamp> statuser = brukerRepository.getAktivitetStatusMap("jegfinnesikke", AktivitetData.aktivitettyperSet);

        statuser.forEach( (key, value) -> {
            assertThat(value).isNull();
            assertThat(statuser).containsKey(key);
        });
    }

    @Test
    public void skalReturnereKorrektStatusPaaAktivitet() {
        Map<String, Boolean> aktivitetTypeTilStatus = new HashMap<>();
        aktivitetTypeTilStatus.put("type1", false);
        aktivitetTypeTilStatus.put("type2", true);

        brukerRepository.upsertAktivitetStatuserForBruker(aktivitetTypeTilStatus, "aktoerid", "personid");
        Map<String, Timestamp> typeTilTimestamp = brukerRepository.getAktivitetStatusMap("personid", ImmutableSet.of("type1", "type2"));

        assertThat(typeTilTimestamp.get("type1")).isNull();
        assertThat(typeTilTimestamp.get("type2")).isNotNull();
    }
}