package no.nav.pto.veilarbportefolje.database;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import no.nav.pto.veilarbportefolje.persononinfo.PersonRepository;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = ApplicationConfigTest.class)
class PersonRepositoryTest {

    private final PersonRepository personRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PersonRepositoryTest(PersonRepository personRepository, JdbcTemplate jdbcTemplate) {
        this.personRepository = personRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void skalIkkeTryneOmBrukerIkkeFinnes() {
        Optional<Personinfo> finnesikke = personRepository.hentPersoninfoForFnr(Fnr.of("11111111111"));
        assertThat(finnesikke).isNotPresent();
    }

    @Test
    void skalHentePersonifoForFnr() {
        Fnr fnr = TestDataUtils.randomFnr();

        SqlUtils.insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
                .value("PERSON_ID", 11111)
                .value("FODSELSNR", fnr.toString())
                .value("SIKKERHETSTILTAK_TYPE_KODE", "FYUS")
                .value("SPERRET_ANSATT", "J")
                .execute();

        Personinfo personinfo = personRepository.hentPersoninfoForFnr(fnr).get();

        assertThat(personinfo.getSikkerhetstiltak()).isEqualTo(Personinfo.kodeTilBeskrivelse.get("FYUS"));
        assertThat(personinfo.isEgenAnsatt()).isTrue();
    }


}
