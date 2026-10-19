package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.client.AktorClient;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;

public class FargekategoriServiceTest {

    private final FargekategoriService fargekategoriService;
    private final JdbcTemplate jdbcTemplate;
    private final PdlIdentRepository pdlIdentRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId enhetId = EnhetId.of("0220");

    public FargekategoriServiceTest() {
        jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate();
        pdlIdentRepository = new PdlIdentRepository(jdbcTemplate);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(jdbcTemplate);
        OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(jdbcTemplate, new NamedParameterJdbcTemplate(jdbcTemplate));
        BrukerServiceV2 brukerServiceV2 = new BrukerServiceV2(pdlIdentRepository, oppfolgingsbrukerRepositoryV3, oppfolgingRepositoryV2, Mockito.mock(AktorClient.class));
        fargekategoriService = new FargekategoriService(new FargekategoriRepository(jdbcTemplate), pdlIdentRepository, Mockito.mock(OpensearchIndexerPaDatafelt.class), brukerServiceV2);
    }

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE fargekategori");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }

    @Test
    public void skalHenteFargekategoriPaaHistoriskFnr() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        lagreIdenter(aktivFnr, historiskFnr);
        lagreFargekategori(historiskFnr, FargekategoriVerdi.FARGEKATEGORI_A, LocalDateTime.now());

        Optional<FargekategoriEntity> fargekategori = fargekategoriService.hentFargekategoriForBruker(new FargekategoriController.HentFargekategoriRequest(aktivFnr));

        assertThat(fargekategori.map(FargekategoriEntity::fargekategoriVerdi)).contains(FargekategoriVerdi.FARGEKATEGORI_A);
    }

    @Test
    public void skalPrioritereFargekategoriPaaAktivFnrFoerSistEndret() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        lagreIdenter(aktivFnr, historiskFnr);
        lagreFargekategori(aktivFnr, FargekategoriVerdi.FARGEKATEGORI_A, LocalDateTime.now().minusDays(2));
        lagreFargekategori(historiskFnr, FargekategoriVerdi.FARGEKATEGORI_B, LocalDateTime.now());

        Optional<FargekategoriEntity> fargekategori = fargekategoriService.hentFargekategoriForBruker(new FargekategoriController.HentFargekategoriRequest(aktivFnr));

        assertThat(fargekategori.map(FargekategoriEntity::fargekategoriVerdi)).contains(FargekategoriVerdi.FARGEKATEGORI_A);
    }

    @Test
    public void batchoppdateringSkalLagrePaaAktivFnrOgSletteFargekategoriPaaHistoriskFnr() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        lagreIdenter(aktivFnr, historiskFnr);
        lagreFargekategori(historiskFnr, FargekategoriVerdi.FARGEKATEGORI_A, LocalDateTime.now());

        fargekategoriService.batchoppdaterFargekategoriForBruker(FargekategoriVerdi.FARGEKATEGORI_C, List.of(aktivFnr), veilederId, enhetId);

        assertThat(hentFnrMedFargekategori()).containsExactly(aktivFnr.get());
        assertThat(fargekategoriService.hentFargekategoriForBruker(new FargekategoriController.HentFargekategoriRequest(aktivFnr)).map(FargekategoriEntity::fargekategoriVerdi))
                .contains(FargekategoriVerdi.FARGEKATEGORI_C);
    }


    @Test
    public void slettFargekategoriPaaBrukerSkalSletteAlleIdenter() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        Fnr annenBruker = randomFnr();
        AktorId aktorId = lagreIdenter(aktivFnr, historiskFnr);
        lagreFargekategori(aktivFnr, FargekategoriVerdi.FARGEKATEGORI_A, LocalDateTime.now());
        lagreFargekategori(historiskFnr, FargekategoriVerdi.FARGEKATEGORI_B, LocalDateTime.now());
        lagreFargekategori(annenBruker, FargekategoriVerdi.FARGEKATEGORI_B, LocalDateTime.now());

        fargekategoriService.slettFargekategoriPaaBruker(aktorId, Optional.of(aktivFnr));

        assertThat(hentFnrMedFargekategori()).containsExactly(annenBruker.get());
    }

    @Test
    public void oppdaterEnhetSkalFlytteFargekategoriFraHistoriskTilAktivFnr() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        lagreIdenter(aktivFnr, historiskFnr);
        lagreFargekategori(historiskFnr, FargekategoriVerdi.FARGEKATEGORI_D, LocalDateTime.now());

        fargekategoriService.oppdaterEnhetPaaFargekategori(aktivFnr, EnhetId.of("0330"), veilederId);

        assertThat(hentFnrMedFargekategori()).containsExactly(aktivFnr.get());
        FargekategoriEntity fargekategori = fargekategoriService.hentFargekategoriForBruker(new FargekategoriController.HentFargekategoriRequest(aktivFnr)).orElseThrow();
        assertThat(fargekategori.fargekategoriVerdi()).isEqualTo(FargekategoriVerdi.FARGEKATEGORI_D);
        assertThat(fargekategori.enhetId()).isEqualTo(EnhetId.of("0330"));
    }

    @Test
    public void brukerHarFargekategoriPaForrigeNavkontorSkalReturnereFalseNaarFargekategoriManglerEnhet() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        AktorId aktorId = lagreIdenter(aktivFnr, historiskFnr);
        jdbcTemplate.update("INSERT INTO fargekategori (id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), historiskFnr.get(), FargekategoriVerdi.FARGEKATEGORI_A.name(), Timestamp.valueOf(LocalDateTime.now()), null, null);

        assertThat(fargekategoriService.brukerHarFargekategoriPaForrigeNavkontor(aktorId, Optional.of(aktivFnr))).isFalse();
    }

    private AktorId lagreIdenter(Fnr aktivFnr, Fnr historiskFnr) {
        AktorId aktorId = randomAktorId();
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktivFnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(historiskFnr.get(), true, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), UUID.randomUUID());
        return aktorId;
    }

    private void lagreFargekategori(Fnr fnr, FargekategoriVerdi verdi, LocalDateTime sistEndret) {
        jdbcTemplate.update("INSERT INTO fargekategori (id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), fnr.get(), verdi.name(), Timestamp.valueOf(sistEndret), veilederId.getValue(), enhetId.get());
    }

    private List<String> hentFnrMedFargekategori() {
        return jdbcTemplate.queryForList("SELECT fnr FROM fargekategori", String.class);
    }
}
