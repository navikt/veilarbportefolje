package no.nav.pto.veilarbportefolje.arbeidsliste;

import jakarta.servlet.ServletException;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.Decision;
import no.nav.pto.veilarbportefolje.arbeidsliste.v1.ArbeidsListeController;
import no.nav.pto.veilarbportefolje.arbeidsliste.v1.ArbeidslisteRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidsListeV2Controller;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteForBrukerRequest;
import no.nav.pto.veilarbportefolje.arbeidsliste.v2.ArbeidslisteV2Request;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.controller.VeilederController;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.generateJWT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = {ArbeidsListeController.class, ArbeidsListeV2Controller.class, VeilederController.class})
@Import(ApplicationConfigTest.class)
class ArbeidslisteIntegrationTest {

    private static final String TEST_FNR = "11111111111";
    private static final String TEST_FNR_2 = "22222222222";
    private static final String TEST_ENHETSID = "1234";
    private static final String TEST_VEILEDERIDENT = "Z123456";
    private static final String TEST_AKTORID = "111111111";
    private static final String TEST_AKTORID_2 = "222222222";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PdlIdentRepository pdlIdentRepository;
    @Autowired
    private AuthContextHolder authContextHolder;
    @Autowired
    private ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    @Autowired
    private JdbcTemplate db;
    @Autowired
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @MockBean
    private AuthService authService;
    @MockBean
    private PoaoTilgangWrapper poaoTilgangWrapper;
    @MockBean
    private VeilarbPep veilarbPep;
    @MockBean
    private BrukerServiceV2 brukerService;
    @MockBean
    private AktorClient aktorClient;

    @Test
    void hent_arbeidsliste_skal_hente_arbeidsliste_som_forventet_naar_fargekategori_tabell_er_populert_v2() {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.GUL.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"Overskriften\",\"kommentar\":\"Kommentaren\",\"frist\":null,\"kategori\":\"GUL\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_ikke_har_arbeidsliste_v1()  {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post(String.format("/api/arbeidsliste/%s", TEST_FNR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteRequest()
                                    .setFnr(TEST_FNR)
                                    .setOverskrift(null)
                                    .setKommentar(null)
                                    .setFrist(null)
                                    .setKategori("LILLA")
                            )))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_ikke_har_arbeidsliste_v2()  {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    "LILLA"))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_har_arbeidsliste_v1()  {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                Arbeidsliste.Kategori.BLA
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post(String.format("/api/arbeidsliste/%s", TEST_FNR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteRequest()
                                    .setFnr(TEST_FNR)
                                    .setOverskrift(null)
                                    .setKommentar(null)
                                    .setFrist(null)
                                    .setKategori("LILLA")
                            )))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_har_arbeidsliste_v2()  {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                Arbeidsliste.Kategori.BLA
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    "LILLA"))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_har_arbeidsliste_og_fargekategori_v1()  {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.GUL.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post(String.format("/api/arbeidsliste/%s", TEST_FNR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteRequest()
                                    .setFnr(TEST_FNR)
                                    .setOverskrift(null)
                                    .setKommentar(null)
                                    .setFrist(null)
                                    .setKategori("LILLA")
                            )))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void opprett_arbeidsliste_skal_lagre_arbeisliste_som_forventet_for_bruker_som_har_arbeidsliste_og_fargekategori_v2() {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.GUL.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    null,
                                    null,
                                    null,
                                    "LILLA"))))
                    .andExpect(
                            MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_returnere_exception_nar_bruker_ikke_har_arbeidsliste_v1()  {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            ServletException exception = assertThrows(ServletException.class,
                    () -> mockMvc.perform(MockMvcRequestBuilders.delete(String.format("/api/arbeidsliste/%s", TEST_FNR)))
            );

            assertThat(exception).hasCauseInstanceOf(IllegalStateException.class);

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_returnere_exception_nar_bruker_ikke_har_arbeidsliste_v2()  {
        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            ServletException exception = assertThrows(ServletException.class,
                    () -> mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
            );

            assertThat(exception).hasCauseInstanceOf(IllegalStateException.class);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_fjerne_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_v1()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.delete(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_fjerne_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_v2()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR))))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_fjerne_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_og_fargekategori_v1()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.delete(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void slett_arbeidsliste_skal_fjerne_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_og_fargekategori_v2()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR))))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":null,\"overskrift\":null,\"kommentar\":null,\"frist\":null,\"kategori\":null,\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":null}"));
        });
    }

    @Test
    void oppdater_arbeidsliste_skal_oppdatere_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_v1()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.put(String.format("/api/arbeidsliste/%s", TEST_FNR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteRequest()
                                    .setOverskrift("En flott tittel")
                                    .setKommentar("Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.")
                                    .setKategori("GRONN")))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void oppdater_arbeidsliste_skal_oppdatere_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_v2()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    "En flott tittel",
                                    "Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.",
                                    null,
                                    "GRONN")))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void oppdater_arbeidsliste_skal_oppdatere_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_og_fargekategori_v1()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.put(String.format("/api/arbeidsliste/%s", TEST_FNR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteRequest()
                                    .setOverskrift("En flott tittel")
                                    .setKommentar("Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.")
                                    .setKategori("GRONN")))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));

            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/arbeidsliste/%s", TEST_FNR)))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void oppdater_arbeidsliste_skal_oppdatere_arbeidsliste_som_forventet_nar_bruker_har_arbeidsliste_og_fargekategori_v2()  {
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                null,
                null,
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteV2Request(Fnr.of(TEST_FNR),
                                    "En flott tittel",
                                    "Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.",
                                    null,
                                    "GRONN")))
                    )
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json("{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/hent-arbeidsliste")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.toJson(new ArbeidslisteForBrukerRequest(Fnr.of(TEST_FNR)))))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"En flott tittel\",\"kommentar\":\"Glemte å legge til kommentar. Nå er det gjort. Trenger ikke frist på denne.\",\"frist\":null,\"kategori\":\"GRONN\",\"isOppfolgendeVeileder\":true,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":true,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"}"));
        });
    }

    @Test
    void hent_arbeidsliste_for_veileder_skal_returnere_arbeidslister_som_forventet_nar_brukere_har_arbeidsliste_v1()  {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity2 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR_2)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity2);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID_2), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID_2), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR_2),
                "Overskriften",
                "Kommentaren",
                null,
                Arbeidsliste.Kategori.LILLA
        ).setAktorId(AktorId.of(TEST_AKTORID_2)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/veileder/%s/hentArbeidslisteForVeileder", TEST_VEILEDERIDENT)).queryParam("enhet", TEST_ENHETSID))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "[{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"Overskriften\",\"kommentar\":\"Kommentaren\",\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":null,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":null,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"},{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"Overskriften\",\"kommentar\":\"Kommentaren\",\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":null,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":null,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"222222222\"}]"));
        });
    }

    @Test
    void hent_arbeidsliste_for_veileder_skal_returnere_arbeidslister_som_forventet_nar_brukere_har_arbeidsliste_og_fargekategori_v1()  {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity1 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity1);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID), VeilederId.of(TEST_VEILEDERIDENT));
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity2 = OppfolgingsbrukerEntity.builder()
                .fodselsnr(TEST_FNR_2)
                .nav_kontor(TEST_ENHETSID)
                .endret_dato(ZonedDateTime.now().minusDays(10))
                .build();
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity2);
        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(TEST_AKTORID_2), ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(AktorId.of(TEST_AKTORID_2), VeilederId.of(TEST_VEILEDERIDENT));
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR),
                "Overskriften",
                "Kommentaren",
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);
        ArbeidslisteRepositoryV2Test.insertArbeidsliste(ArbeidslisteDTO.of(
                Fnr.of(TEST_FNR_2),
                "Overskriften",
                "Kommentaren",
                null,
                null
        ).setAktorId(AktorId.of(TEST_AKTORID_2)).setVeilederId(VeilederId.of(TEST_VEILEDERIDENT)), db);
        db.update("""
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                VALUES(?,?,?,?,?)
                """, UUID.randomUUID(), TEST_FNR_2, FargekategoriVerdi.LILLA.verdi, Timestamp.valueOf(LocalDateTime.now()), TEST_VEILEDERIDENT);

        authContextHolder.withContext(new AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDERIDENT)), () -> {
            mockMvc.perform(MockMvcRequestBuilders.get(String.format("/api/veileder/%s/hentArbeidslisteForVeileder", TEST_VEILEDERIDENT)).queryParam("enhet", TEST_ENHETSID))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                            MockMvcResultMatchers.content().json(
                                    "[{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"Overskriften\",\"kommentar\":\"Kommentaren\",\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":null,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":null,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"111111111\"},{\"sistEndretAv\":{\"veilederId\":\"Z123456\"},\"overskrift\":\"Overskriften\",\"kommentar\":\"Kommentaren\",\"frist\":null,\"kategori\":\"LILLA\",\"isOppfolgendeVeileder\":null,\"arbeidslisteAktiv\":null,\"harVeilederTilgang\":null,\"navkontorForArbeidsliste\":null,\"aktoerid\":\"222222222\"}]"));
        });
    }

    @AfterAll
    static void clear(
            @Autowired JdbcTemplate db
    ) {
        db.update("TRUNCATE arbeidsliste");
        db.update("TRUNCATE fargekategori");
        db.update("TRUNCATE bruker_identer");
        db.update("TRUNCATE oppfolgingsbruker_arena_v2");
    }

    @BeforeEach
    void setupMocks() {
        db.update("TRUNCATE arbeidsliste");
        db.update("TRUNCATE fargekategori");
        db.update("TRUNCATE bruker_identer");
        db.update("TRUNCATE oppfolgingsbruker_arena_v2");

        when(poaoTilgangWrapper.harVeilederTilgangTilModia()).thenReturn(Decision.Permit.INSTANCE);
        when(poaoTilgangWrapper.harTilgangTilPerson(any())).thenReturn(Decision.Permit.INSTANCE);
        when(veilarbPep.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        when(veilarbPep.harTilgangTilPerson(any(), any(), any())).thenReturn(true);
        when(authService.harVeilederTilgangTilEnhet(any(), any())).thenReturn(true);
        Mockito.doNothing().when(authService).tilgangTilOppfolging();
        when(brukerService.hentVeilederForBruker(AktorId.of(TEST_AKTORID))).thenReturn(Optional.of(VeilederId.of(
                TEST_VEILEDERIDENT)));
        when(brukerService.hentVeilederForBruker(AktorId.of(TEST_AKTORID_2))).thenReturn(Optional.of(VeilederId.of(
                TEST_VEILEDERIDENT)));
        when(brukerService.hentNavKontor(Fnr.of(TEST_FNR))).thenReturn(Optional.of(NavKontor.of(TEST_ENHETSID)));
        when(brukerService.hentNavKontor(Fnr.of(TEST_FNR_2))).thenReturn(Optional.of(NavKontor.of(TEST_ENHETSID)));
        when(aktorClient.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(AktorId.of(TEST_AKTORID));
        when(aktorClient.hentAktorId(Fnr.of(TEST_FNR_2))).thenReturn(AktorId.of(TEST_AKTORID_2));
        when(brukerService.hentAktorId(Fnr.of(TEST_FNR))).thenReturn(Optional.of(AktorId.of(TEST_AKTORID)));
        when(brukerService.hentAktorId(Fnr.of(TEST_FNR_2))).thenReturn(Optional.of(AktorId.of(TEST_AKTORID_2)));

        PDLIdent pdlIdentFnr1 = PDLIdent.builder().ident(TEST_FNR).gruppe(PDLIdent.Gruppe.FOLKEREGISTERIDENT).historisk(false).build();
        PDLIdent pdlIdentAktorId1 = PDLIdent.builder().ident(TEST_AKTORID).gruppe(PDLIdent.Gruppe.AKTORID).historisk(false).build();
        PDLIdent pdlIdentFnr2 = PDLIdent.builder().ident(TEST_FNR_2).gruppe(PDLIdent.Gruppe.FOLKEREGISTERIDENT).historisk(false).build();
        PDLIdent pdlIdentAktorId2 = PDLIdent.builder().ident(TEST_AKTORID_2).gruppe(PDLIdent.Gruppe.AKTORID).historisk(false).build();
        pdlIdentRepository.upsertIdenter(List.of(pdlIdentFnr1, pdlIdentAktorId1));
        pdlIdentRepository.upsertIdenter(List.of(pdlIdentFnr2, pdlIdentAktorId2));
    }
}
