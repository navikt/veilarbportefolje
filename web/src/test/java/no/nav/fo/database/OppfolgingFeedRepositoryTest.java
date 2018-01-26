package no.nav.fo.database;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class OppfolgingFeedRepositoryTest {

    private JdbcTemplate db;
    private DataSource ds;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private BrukerRepository brukerRepository;

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        oppfolgingFeedRepository = new OppfolgingFeedRepository(db);
        brukerRepository = new BrukerRepository(db,ds,namedParameterJdbcTemplate);
    }

    @Test
    public void skalInserteOppfolgingsdata() {
        String veilederid = "X000000";
        PersonId personId = PersonId.of("1111");
        AktoerId aktoerId = AktoerId.of("0000");
        Timestamp tildeltTidspunkt = new Timestamp(Instant.now().toEpochMilli());

        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon()
                .setAktoerid(aktoerId.toString())
                .setOppfolging(true)
                .setVeileder(veilederid)
                .setEndretTimestamp(tildeltTidspunkt);

        oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker, personId);
        Brukerdata brukerdata = brukerRepository.retrieveBrukerdata(Collections.singletonList(personId.toString())).get(0);
        assertThat(brukerdata.getVeileder()).isEqualTo(veilederid);
    }

    @Test
    public void skalUpserteOppfolgingsdata() {
        String veilederid = "X000000";
        PersonId personId = PersonId.of("1111");
        AktoerId aktoerId = AktoerId.of("0000");
        Timestamp tildeltTidspunkt = new Timestamp(Instant.now().toEpochMilli());


        Brukerdata brukerdata = new Brukerdata()
                .setPersonid(personId.toString())
                .setVeileder("Z000000")
                .setAktoerid(aktoerId.toString())
                .setTildeltTidspunkt(tildeltTidspunkt)
                .setNyesteUtlopteAktivitet(new Timestamp(0))
                .setAktivitetStart(new Timestamp(1))
                .setNesteAktivitetStart(new Timestamp(2))
                .setForrigeAktivitetStart(new Timestamp(3))
                .setOppfolging(false);

        brukerRepository.insertOrUpdateBrukerdata(Collections.singletonList(brukerdata), Collections.emptyList());

        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon()
                .setAktoerid(aktoerId.toString())
                .setOppfolging(true)
                .setNyForVeileder(true)
                .setVeileder(veilederid)
                .setEndretTimestamp(tildeltTidspunkt);

        oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker, personId);

        Brukerdata oppdatertBrukerdata = brukerRepository.retrieveBrukerdata(Collections.singletonList(personId.toString())).get(0);
        assertThat(oppdatertBrukerdata).isEqualTo(brukerdata.setVeileder(veilederid).setOppfolging(true).setNyForVeileder(true));

    }

}