package no.nav.fo.database;

import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.Personinfo;
import no.nav.fo.util.sql.SqlUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;

import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.database.BrukerRepository.OPPFOLGINGSBRUKER;
import static no.nav.fo.database.PersonRepository.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PersonRepositoryTest {
    private DataSource ds;
    private JdbcTemplate db;
    private PersonRepository personRepository;

    @BeforeEach
    public void setup() {
        ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        personRepository = new PersonRepository(ds);
    }

    @AfterEach
    public void tearDown() {
        db.execute("TRUNCATE TABLE OPPFOLGINGSBRUKER");
    }

    @Test
    public void skalIkkeTryneOmBrukerIkkeFinnes() {
        Optional<Personinfo> finnesikke = personRepository.hentPersoninfoForFnr(Fnr.of("11111111111"));
        assertThat(finnesikke.isPresent()).isFalse();
    }

    @Test
    public void skalHentePersonifoForFnr() {
        Fnr fnr = Fnr.of("10109012345");
        Timestamp dodsdato = new Timestamp(0);

        SqlUtils.insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", 11111)
                .value(FODSELSNR, fnr.toString())
                .value(SIKKERHETSTILTAK_TYPE_KODE, "FYUS")
                .value(SPERRET_ANSATT, "J")
                .value(FR_KODE, "7")
                .value(ETTERNAVN, "ETTERNAVN")
                .value(FORNAVN, "FORNAVN")
                .value(DOED_FRA_DATO, dodsdato)
                .execute();

        Personinfo personinfo = personRepository.hentPersoninfoForFnr(fnr).get();

        assertThat(personinfo.getSikkerhetstiltak()).isEqualTo(Personinfo.kodeTilBeskrivelse.get("FYUS"));
        assertThat(personinfo.isEgenAnsatt()).isTrue();
        assertThat(personinfo.getDiskresjonskode()).isEqualTo("7");
        assertThat(personinfo.getEtternavn()).isEqualTo("ETTERNAVN");
        assertThat(personinfo.getFornavn()).isEqualTo("FORNAVN");
        assertThat(personinfo.getKjonn()).isEqualTo("M");
        assertThat(personinfo.getFodselsdato()).isEqualTo(LocalDate.of(1990,10,10));
        assertThat(personinfo.getFodselsnummer()).isEqualTo("10109012345");
    }

    @Test
    public void skalBareSetteDiskresjonskodeOmDenEr6Eller7() {
        Fnr fnr = Fnr.of("10109012345");

        SqlUtils.insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", 11111)
                .value(FODSELSNR, fnr.toString())
                .value(FR_KODE, "5")
                .execute();

        Personinfo personinfo = personRepository.hentPersoninfoForFnr(fnr).get();

        assertThat(personinfo.getDiskresjonskode()).isNull();
    }

    @Test
    public void skalSetteDiskresjonskode6() {
        Fnr fnr = Fnr.of("10109012345");

        SqlUtils.insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", 11111)
                .value(FODSELSNR, fnr.toString())
                .value(FR_KODE, "6")
                .execute();

        Personinfo personinfo = personRepository.hentPersoninfoForFnr(fnr).get();

        assertThat(personinfo.getDiskresjonskode()).isEqualTo("6");
    }

    @Test
    public void skalSetteDiskresjonskode7() {
        Fnr fnr = Fnr.of("10109012345");

        SqlUtils.insert(db, OPPFOLGINGSBRUKER)
                .value("PERSON_ID", 11111)
                .value(FODSELSNR, fnr.toString())
                .value(FR_KODE, "7")
                .execute();

        Personinfo personinfo = personRepository.hentPersoninfoForFnr(fnr).get();

        assertThat(personinfo.getDiskresjonskode()).isEqualTo("7");
    }


}