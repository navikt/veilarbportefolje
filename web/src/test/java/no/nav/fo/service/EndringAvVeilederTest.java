package no.nav.fo.service;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.PersonId;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.sql.Timestamp;

import static no.nav.fo.mock.AktoerServiceMock.*;
import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.reset;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class EndringAvVeilederTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    AktoerV2 aktoerV2;

    @Inject
    BrukerRepository brukerRepository;

    @Inject
    OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Before
    public void beforeTests() {
        reset(aktoerV2);
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table bruker_data");
        jdbcTemplate.execute("truncate table AKTOERID_TO_PERSONID");
    }

    @Test
    public void skalHenteFraAktoerLagrePersonidIDBogOppdatererBrukerdata() {
        insert(jdbcTemplate, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", AKTOER_ID)
                .value("PERSONID", PERSON_ID)
                .execute();

        updateDBWithPersonidbruker(PERSON_ID, FNR);

        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon()
                .setAktoerid(AKTOER_ID)
                .setVeileder("X111111")
                .setEndretTimestamp(Timestamp.valueOf("2017-01-14 13:33:16.000000"));

        oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(bruker, new PersonId(PERSON_ID));

        String veileder = (String) brukerRepository.retrieveBruker(AKTOER_ID).get(0).get("VEILEDERIDENT");
        String personid = (String) brukerRepository.retrievePersonid(AKTOER_ID).get(0).get("PERSONID");

        assertThat(personid).isEqualTo(PERSON_ID);
        assertThat(veileder).isEqualTo("X111111");
    }

    private void updateDBWithPersonidbruker(String personid, String fnr) {
        jdbcTemplate.execute("INSERT INTO OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR, ETTERNAVN, FORNAVN, NAV_KONTOR, FORMIDLINGSGRUPPEKODE, " +
                "ISERV_FRA_DATO, KVALIFISERINGSGRUPPEKODE, RETTIGHETSGRUPPEKODE, HOVEDMAALKODE, SIKKERHETSTILTAK_TYPE_KODE, FR_KODE, " +
                "SPERRET_ANSATT, ER_DOED, DOED_FRA_DATO, TIDSSTEMPEL) VALUES " +
                "(" + personid + ", '" + fnr + "', 'GAASEN', 'GUNNAR', '0713', 'ARBS', null, 'BATT', 'IYT', 'SKAFFEA', 'TOAN', '7', 'J', 'N', null, " +
                "TO_TIMESTAMP('2017-01-13 14:59:29.000000', 'YYYY-MM-DD HH24:MI:SS.FF'));\n");
    }

}
