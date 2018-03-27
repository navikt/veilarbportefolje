package no.nav.fo.service;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.sql.SqlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.vavr.control.Try;

import javax.inject.Inject;
import java.util.Optional;

import static no.nav.fo.util.sql.SqlUtils.insert;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktoerServiceImplTest {

    private static final String AKTOER_ID = "aktoerid1";
    private static final String FNR = "00000000000";
    private static final String PERSON_ID = "111111";

    @Inject
    private AktorService aktorService;

    @Inject
    private AktoerServiceImpl aktoerService;

    @Inject
    private JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    @Before
    public void setUp() {
        reset(aktorService);
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("truncate table AKTOERID_TO_PERSONID");
    }

    @Test
    public void skalKalleHenteAktoeridViaSoapOgInsertIDb() throws Exception {
        PersonId personId = PersonId.of(PERSON_ID);
        AktoerId aktoerId = AktoerId.of(AKTOER_ID);

        SqlUtils.insert(db, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", new Integer(PERSON_ID))
                .value("FODSELSNR", FNR)
                .execute();
        when(aktorService.getAktorId(anyString())).thenReturn(Optional.of(aktoerId.toString()));
        aktoerService.hentAktoeridFraPersonid(personId);
        verify(aktorService, times(1)).getAktorId(any());

        assertThat(brukerRepository.retrievePersonid(aktoerId).get()).isEqualTo(personId);
    }

    @Test
    public void skalHenteAktoerIdFraPersonId() throws Exception {
        PersonId personId = PersonId.of(PERSON_ID);
        AktoerId aktoerId = AktoerId.of(AKTOER_ID);
        int updated = insert(db, "AKTOERID_TO_PERSONID")
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .execute();

        assertTrue(updated > 0);

        Try<AktoerId> result = aktoerService.hentAktoeridFraPersonid(personId);
        assertTrue(result.isSuccess());
        assertEquals(aktoerId, result.get());
    }

}