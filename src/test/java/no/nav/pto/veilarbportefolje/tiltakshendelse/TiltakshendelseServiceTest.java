package no.nav.pto.veilarbportefolje.tiltakshendelse;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Avsender;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

class TiltakshendelseServiceTest extends EndToEndTest {
    @Autowired
    private TiltakshendelseRepository repository;
    @Autowired
    private TiltakshendelseService service;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private BrukerServiceV2 brukerServiceV2;
    @MockBean
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tiltakshendelse");
    }

    @Test
    public void kanMottaTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");
        AktorId aktorId = AktorId.of("112233445566");
        when(brukerServiceV2.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));
        when(oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktorId)).thenReturn(Boolean.TRUE);

        opensearchTestClient.createUserInOpensearch(aktorId);
        final OppfolgingsBruker bruker = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(bruker.getAktoer_id()).isEqualTo(aktorId.get());
        assertThat(bruker.getTiltakshendelse()).isNull();

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        service.behandleKafkaMeldingLogikk(kafkaData);

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().id().equals(expected.id()));

        final OppfolgingsBruker brukerEtterKafkaMelding = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(brukerEtterKafkaMelding.getTiltakshendelse().id()).isEqualTo(expected.id());
    }

    @Test
    public void kanMottaOgInaktivereTiltakshendelse() {
        UUID id = UUID.randomUUID();
        LocalDateTime opprettet = LocalDateTime.now();
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");
        AktorId aktorId = AktorId.of("112233445566");
        when(brukerServiceV2.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));
        when(oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktorId)).thenReturn(Boolean.TRUE);

        opensearchTestClient.createUserInOpensearch(aktorId);
        final OppfolgingsBruker bruker = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(bruker.getAktoer_id()).isEqualTo(aktorId.get());
        assertThat(bruker.getTiltakshendelse()).isNull();

        KafkaTiltakshendelse kafkaData = new KafkaTiltakshendelse(id, true, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        Tiltakshendelse expected = new Tiltakshendelse(id, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr);

        service.behandleKafkaMeldingLogikk(kafkaData);

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 1);
        assert (tiltakshendelser.getFirst().id().equals(expected.id()));
        final OppfolgingsBruker brukerEtterKafkaMelding = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(brukerEtterKafkaMelding.getTiltakshendelse().id()).isEqualTo(expected.id());

        KafkaTiltakshendelse slettKafkaData = new KafkaTiltakshendelse(id, false, opprettet, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        service.behandleKafkaMeldingLogikk(slettKafkaData);

        List<Tiltakshendelse> tiltakshendelserEtterSlett = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelserEtterSlett.isEmpty());
        final OppfolgingsBruker brukerEtterSlettIOpensearch = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(brukerEtterSlettIOpensearch.getTiltakshendelse()).isNull();
    }

    @Test
    public void kanInaktivereTiltakshendelseOgOppdatereOpensearchMedEldsteTiltakshendelse() {
        UUID hendelseId1 = UUID.randomUUID();
        UUID hendelseId2 = UUID.randomUUID();
        LocalDateTime opprettetHendelse1 = LocalDateTime.now();
        LocalDateTime opprettetHendelse2 = LocalDateTime.now().plusDays(1);
        String tekst = "Forslag: endre varighet";
        String lenke = "http.cat/200";
        Fnr fnr = Fnr.of("11223312345");
        AktorId aktorId = AktorId.of("112233445566");
        when(brukerServiceV2.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));
        when(oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktorId)).thenReturn(Boolean.TRUE);

        opensearchTestClient.createUserInOpensearch(aktorId);
        final OppfolgingsBruker bruker = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(bruker.getAktoer_id()).isEqualTo(aktorId.get());
        assertThat(bruker.getTiltakshendelse()).isNull();

        KafkaTiltakshendelse kafkaDataHendelse1 = new KafkaTiltakshendelse(hendelseId1, true, opprettetHendelse1, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        KafkaTiltakshendelse kafkaDataHendelse2 = new KafkaTiltakshendelse(hendelseId2, true, opprettetHendelse2, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);

        service.behandleKafkaMeldingLogikk(kafkaDataHendelse1);
        service.behandleKafkaMeldingLogikk(kafkaDataHendelse2);

        List<Tiltakshendelse> tiltakshendelser = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelser.size() == 2);

        final OppfolgingsBruker brukerEtterKafkaMelding = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(brukerEtterKafkaMelding.getTiltakshendelse().id()).isEqualTo(hendelseId1);

        KafkaTiltakshendelse slettKafkaData = new KafkaTiltakshendelse(hendelseId1, false, opprettetHendelse1, tekst, lenke, Tiltakstype.ARBFORB, fnr, Avsender.KOMET);
        service.behandleKafkaMeldingLogikk(slettKafkaData);

        List<Tiltakshendelse> tiltakshendelserEtterSlett = repository.hentAlleTiltakshendelser();
        assert (tiltakshendelserEtterSlett.size() == 1);
        final OppfolgingsBruker brukerEtterSlettIOpensearch = opensearchTestClient.hentBrukerFraOpensearch(aktorId);
        assertThat(brukerEtterSlettIOpensearch.getTiltakshendelse().id()).isEqualTo(hendelseId2);
    }
}
