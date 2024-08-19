package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Avsender;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = ApplicationConfigTest.class)
class TiltakshendelseRepositoryTest {
    @Autowired
    private TiltakshendelseRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tiltakshendelse");
    }


    @Test
    public void kanLagreTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseData(kafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().id().equals(expected.id()));
    }

    @Test
    public void kanOppdatereTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse gammelKafkaData = new KafkaTiltakshendelse(id, true, opprettet, "Gamal tekst her", "Gamal lenke her", Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse oppdatertKafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseData(gammelKafkaData));
        assertTrue(repository.tryLagreTiltakshendelseData(oppdatertKafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().tekst().equals(expected.tekst()));
    }

    @Test
    public void kanLagreFlereTiltakshendelserPaSammePerson() {
        UUID id = UUID.randomUUID();
        UUID idNyMelding = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse hendelsePaEnPerson = new KafkaTiltakshendelse(id, true, opprettet, tekst, tekst, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse nyHendelsePaSammePerson = new KafkaTiltakshendelse(idNyMelding, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        assertTrue(repository.tryLagreTiltakshendelseData(hendelsePaEnPerson));
        assertTrue(repository.tryLagreTiltakshendelseData(nyHendelsePaSammePerson));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 2);
    }
}
