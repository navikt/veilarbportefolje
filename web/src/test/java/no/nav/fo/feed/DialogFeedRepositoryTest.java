package no.nav.fo.feed;

import no.nav.fo.database.BrukerRepository;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;


public class DialogFeedRepositoryTest {

    private DataSource ds;
    private JdbcTemplate db;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private BrukerRepository brukerRepository;
    private DialogFeedRepository dialogFeedRepository;

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        brukerRepository = new BrukerRepository(db,ds,namedParameterJdbcTemplate);
        dialogFeedRepository = new DialogFeedRepository(db);
    }

    @Test
    public void skalInserteDialogdata() {
        PersonId personId = PersonId.of("0000");
        AktoerId aktoerId = AktoerId.of("1111");
        Timestamp date = new Timestamp(Instant.now().toEpochMilli());

        DialogDataFraFeed dialogDataFraFeed = new DialogDataFraFeed()
                .setAktorId(aktoerId.toString())
                .setTidspunktEldsteUbehandlede(date)
                .setTidspunktEldsteVentende(date);

        dialogFeedRepository.upsertDialogdata(dialogDataFraFeed, personId);
        Brukerdata brukerdata = brukerRepository.retrieveBrukerdata(Collections.singletonList(personId.toString())).get(0);
        assertThat(brukerdata.getVenterPaSvarFraBruker(), is(date.toLocalDateTime()));
    }

    @Test
    public void skalUpserteDialogdata() {
        PersonId personId = PersonId.of("0000");
        AktoerId aktoerId = AktoerId.of("1111");
        Timestamp eldsteVentende1 = new Timestamp(0);
        Timestamp eldsteVentende2 = new Timestamp(Instant.now().toEpochMilli());

        Brukerdata brukerdata = new Brukerdata()
                .setPersonid(personId.toString())
                .setAktoerid(aktoerId.toString())
                .setVenterPaSvarFraNav(eldsteVentende1.toLocalDateTime());

        brukerRepository.insertOrUpdateBrukerdata(Collections.singletonList(brukerdata), Collections.emptyList());

        DialogDataFraFeed dialogDataFraFeed = new DialogDataFraFeed()
                .setAktorId(aktoerId.toString())
                .setTidspunktEldsteUbehandlede(eldsteVentende2);

        dialogFeedRepository.upsertDialogdata(dialogDataFraFeed, personId);

        Brukerdata brukerdataUpdated = brukerRepository.retrieveBrukerdata(Collections.singletonList(personId.toString())).get(0);
        assertThat(brukerdataUpdated, is(brukerdata.setVenterPaSvarFraNav(eldsteVentende2.toLocalDateTime())));
    }
    
    @Test
    public void oppdaterDialogInfoForBruker_skal_sette_inn_i_egen_tabell_og_vare_tilgjengelig_i_dialogview() {
        AktoerId aktoerId = AktoerId.of("1111");
        Timestamp date = new Timestamp(Instant.now().toEpochMilli());

        DialogDataFraFeed dialogDataFraFeed = new DialogDataFraFeed()
                .setAktorId(aktoerId.toString())
                .setTidspunktEldsteUbehandlede(date)
                .setSisteEndring(date)
                .setTidspunktEldsteVentende(date);

        dialogFeedRepository.oppdaterDialogInfoForBruker(dialogDataFraFeed);
        DialogDataFraFeed dialogFraDatabase =  dialogFeedRepository.retrieveDialogData(aktoerId.toString()).get();
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende(), is(date));
        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede(), is(date));
        assertThat(dialogFraDatabase.getSisteEndring(), is(date));
        assertThat(dialogFraDatabase.getAktorId(), is(aktoerId.toString()));
    }

}