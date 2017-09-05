package no.nav.fo.service;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.domene.Fnr;
import no.nav.fo.util.sql.SqlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.assertj.core.api.Java6Assertions.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktoerServiceImplTest {

    @Inject
    AktoerService aktoerService;

    @Inject
    JdbcTemplate db;

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

}