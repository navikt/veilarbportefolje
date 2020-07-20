package no.nav.pto.veilarbportefolje.database;

import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.Personinfo;
import no.nav.pto.veilarbportefolje.persononinfo.PersonRepository;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PersonRepositoryTest {
    private DataSource ds;
    private JdbcTemplate db;
    private PersonRepository personRepository;

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        personRepository = new PersonRepository(db);
    }

    @Test
    public void skalIkkeTryneOmBrukerIkkeFinnes() {
        Optional<Personinfo> finnesikke = personRepository.hentPersoninfoForFnr(Fnr.of("11111111111"));
        assertThat(finnesikke.isPresent()).isFalse();
    }

    @Test
    public void skalHentePersonifoForFnr() {
        Fnr fnr = Fnr.of("00000000000");

        SqlUtils.insert(db, "OPPFOLGINGSBRUKER")
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
