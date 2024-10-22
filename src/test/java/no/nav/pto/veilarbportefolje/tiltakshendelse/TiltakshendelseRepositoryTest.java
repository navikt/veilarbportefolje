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
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tiltakshendelse");
    }


    @Test
    void kanLagreTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(kafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().id().equals(expected.id()));
    }

    @Test
    void kanOppdatereTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse gammelKafkaData = new KafkaTiltakshendelse(id, true, opprettet, "Gamal tekst her", "Gamal lenke her", Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse oppdatertKafkaData = new KafkaTiltakshendelse(id, true, opprettet.plusDays(1), tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(gammelKafkaData));
        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(oppdatertKafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().tekst().equals(expected.tekst()));
    }

    @Test
    void kanIkkeOppdatereTiltakshendelseEtterEndretFnr() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        String oppdatertTekst = "Forslag: endre utkast";
        Fnr fnr = Fnr.of("11223312345");
        Fnr nyttFnr = Fnr.of("12345112233");

        KafkaTiltakshendelse gammelKafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse oppdatertKafkaData = new KafkaTiltakshendelse(id, true, opprettet.plusDays(1), oppdatertTekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse oppdatertMedNyttFnrKafkaData = new KafkaTiltakshendelse(id, true, opprettet.plusDays(2), tekst, lenke, Tiltakstype.ARBFORB, nyttFnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet.plusDays(1), oppdatertTekst, lenke, Tiltakstype.ARBFORB, fnr);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(gammelKafkaData));
        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(oppdatertKafkaData));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().tekst().equals(expected.tekst()));

        assertThrowsExactly(TiltakshendelseTilhorerIkkePersonException.class, () -> repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(oppdatertMedNyttFnrKafkaData));
    }

    @Test
    void kanLagreFlereTiltakshendelserPaSammePerson() {
        UUID id = UUID.randomUUID();
        UUID idNyMelding = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        LocalDateTime opprettetNyMelding = LocalDateTime.now().plusDays(1);
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse hendelsePaEnPerson = new KafkaTiltakshendelse(id, true, opprettet, tekst, tekst, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse nyHendelsePaSammePerson = new KafkaTiltakshendelse(idNyMelding, true, opprettetNyMelding, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(hendelsePaEnPerson));
        assertFalse(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(nyHendelsePaSammePerson));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 2);
    }

    @Test
    void kanSletteTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse hendelsePaEnPerson = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(hendelsePaEnPerson));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);

        assertNull(repository.slettTiltakshendelseOgHentEldste(id, fnr));

        List<Tiltakshendelse> tiltakshendelserEtterInaktivMelding = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelserEtterInaktivMelding.isEmpty());
    }

    @Test
    void kanSletteTiltakshendelseReturnereNesteEldste() {
        UUID id = UUID.randomUUID();
        UUID idNyMelding = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        LocalDateTime opprettetNyMelding = LocalDateTime.now().plusDays(1);
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        KafkaTiltakshendelse hendelsePaEnPerson = new KafkaTiltakshendelse(id, true, opprettet, tekst, tekst, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse nyHendelsePaSammePerson = new KafkaTiltakshendelse(idNyMelding, true, opprettetNyMelding, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(hendelsePaEnPerson));
        assertFalse(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(nyHendelsePaSammePerson));

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 2);

        Tiltakshendelse eldsteTiltakshendelse = repository.slettTiltakshendelseOgHentEldste(id, fnr);
        assert (eldsteTiltakshendelse != null);
        assertEquals(eldsteTiltakshendelse.id(), idNyMelding);
    }

    @Test
    public void kanHenteTiltakshendelseMedEldsteOpprettetdato() {
        Fnr fnr = Fnr.of("11223312345");

        UUID id1 = UUID.randomUUID();
        LocalDateTime opprettet1 = LocalDateTime.of(2024, 6, 1, 12, 0);

        UUID id2 = UUID.randomUUID();
        LocalDateTime opprettet2 = LocalDateTime.of(2022, 6, 1, 12, 0);

        UUID id3 = UUID.randomUUID();
        LocalDateTime opprettet3 = LocalDateTime.of(2020, 6, 1, 12, 0);

        KafkaTiltakshendelse tiltakshendelse1 = new KafkaTiltakshendelse(id1, true, opprettet1, "Forslag: endre varighet", "http.cat/204", Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse tiltakshendelse2 = new KafkaTiltakshendelse(id2, true, opprettet2, "Forslag: endre varighet", "http.cat/204", Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse tiltakshendelse3 = new KafkaTiltakshendelse(id3, true, opprettet3, "Forslag: endre varighet", "http.cat/204", Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        // Lagrar hendelsane i motsett rekkefølgje av oppretta-tidspunktet
        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelse1));
        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelse2));
        assertTrue(repository.tryLagreTiltakshendelseOgSjekkOmDenErEldst(tiltakshendelse3));

        Tiltakshendelse eldsteTiltakshendelse = repository.hentEldsteTiltakshendelse(fnr);
        assert (eldsteTiltakshendelse.id().equals(id3));
    }

    @Test
    public void kanBeOmEldsteHendelseForBrukerUtenHendelse() {
        Fnr fnr = Fnr.of("11223312345");

        assertDoesNotThrow(() ->repository.hentEldsteTiltakshendelse(fnr));
    }
}
