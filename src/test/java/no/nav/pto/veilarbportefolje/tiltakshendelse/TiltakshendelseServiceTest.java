package no.nav.pto.veilarbportefolje.tiltakshendelse;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Avsender;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
class TiltakshendelseServiceTest {
    @Autowired
    private TiltakshendelseRepository repository;
    @Autowired
    private TiltakshendelseService service;
    @MockBean
    private BrukerServiceV2 brukerServiceV2;
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tiltakshendelse");
    }


    @Test
    public void kanTaIMotTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");

        when(brukerServiceV2.hentAktorId(any())).thenReturn(Optional.of(AktorId.of("1233 :)'"))); // Denne burde kanskje skrivast om når vi oppdaterer opensearch også med data.

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        service.behandleKafkaMeldingLogikk(kafkaData);

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().id().equals(expected.id()));
    }
}
