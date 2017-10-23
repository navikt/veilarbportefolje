package no.nav.fo.service;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.sql.SqlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktoerServiceImplTest {

    @Inject
    AktorService aktorService;

    @Inject
    AktoerService aktoerService;

    @Inject
    JdbcTemplate db;

    @Inject
    BrukerRepository brukerRepository;

    @Before
    public void setUp() {
        reset(aktorService);
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
        db.execute("truncate table AKTOERID_TO_PERSONID");
    }

    @Test
    public void skalHentePersonidFraFnr() {
        Fnr fnr = new Fnr("00000000000");

        SqlUtils.insert(db, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", 111111)
                .value("FODSELSNR", fnr.toString())
                .execute();

        String personId = aktoerService.hentPersonidFromFnr(fnr).get().toString();

        assertThat(personId).isEqualTo("111111");
    }

    @Test
    public void skalKalleHenteAktoeridViaSoapOgInsertIDb() throws Exception {
        PersonId personId = PersonId.of("111111");
        Fnr fnr1 = Fnr.of("00000000000");
        AktoerId aktoerId = AktoerId.of("aktoerid1");

        SqlUtils.insert(db, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", 111111)
                .value("FODSELSNR", fnr1.toString())
                .execute();
        when(aktorService.getAktorId(anyString())).thenReturn(Optional.of(aktoerId.toString()));
        aktoerService.hentAktoeridFraPersonid(personId);
        verify(aktorService, times(1)).getAktorId(any());

        assertThat(brukerRepository.retrievePersonid(aktoerId).get()).isEqualTo(personId);
    }

}