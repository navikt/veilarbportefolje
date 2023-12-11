package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.ThreadLocalRandom.current;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.FARGEKATEGORI;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArbeidslisteRepositoryV2Test {
    @Autowired
    private ArbeidslisteRepositoryV2 repo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public final static ArbeidslisteDTO TEST_ARBEIDSLISTE_1 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
            .setAktorId(AktorId.of("22222222"))
            .setVeilederId(VeilederId.of("X11111"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 1 kommentar")
            .setOverskrift("Dette er en overskrift")
            .setKategori(Arbeidsliste.Kategori.BLA);

    public final static ArbeidslisteDTO TEST_ARBEIDSLISTE_1_OPPDATERT = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
            .setAktorId(AktorId.of("22222222"))
            .setVeilederId(VeilederId.of("X11111"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 1 oppdatert kommentar")
            .setOverskrift("Dette er en oppdatert overskrift")
            .setKategori(Arbeidsliste.Kategori.GRONN);

    public final static ArbeidslisteDTO TEST_ARBEIDSLISTE_2 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101011"))
            .setAktorId(AktorId.of("22222223"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 2 kommentar")
            .setKategori(Arbeidsliste.Kategori.GRONN);

    public final static ArbeidslisteDTO TEST_ARBEIDSLISTE_3 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101015"))
            .setAktorId(AktorId.of("22222224"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 3 kommentar")
            .setOverskrift("Arbeidsliste tittel")
            .setKategori(Arbeidsliste.Kategori.GRONN);

    private final ArbeidslisteDTO TEST_ARBEIDSLISTE_UTEN_KOMMENTAR = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101012"))
            .setAktorId(AktorId.of("22222225"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar(null)
            .setKategori(Arbeidsliste.Kategori.GRONN)
            .setOverskrift("Dette er en overskrift");

    private final ArbeidslisteDTO TEST_ARBEIDSLISTE_UTEN_TITTEL = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101013"))
            .setAktorId(AktorId.of("22222226"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 3 kommentar")
            .setKategori(Arbeidsliste.Kategori.GRONN)
            .setOverskrift(null);

    private final ArbeidslisteDTO TEST_ARBEIDSLISTE_UTEN_KOMMENTAR_OG_TITTEL = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101014"))
            .setAktorId(AktorId.of("22222227"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar(null)
            .setKategori(Arbeidsliste.Kategori.GRONN)
            .setOverskrift(null);

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE arbeidsliste");
        jdbcTemplate.execute("TRUNCATE TABLE fargekategori");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }

    @AfterAll
    public static void cleanUp(
            @Autowired JdbcTemplate jdbcTemplate
    ) {
        jdbcTemplate.execute("TRUNCATE TABLE arbeidsliste");
        jdbcTemplate.execute("TRUNCATE TABLE fargekategori");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }

    @Test
    void skalKunneHenteArbeidsliste() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_1.getFnr());

        assertThat(result.isSuccess()).isTrue();
        assertThat(TEST_ARBEIDSLISTE_1.getVeilederId()).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    void skalKunneHenteArbeidslisteUtenTittelEllerKommentar() {
        insertArbeidslister();

        Try<Arbeidsliste> resultUtenTittelData = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_UTEN_TITTEL.getFnr());
        Try<Arbeidsliste> resultUtenKommentarData = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_UTEN_KOMMENTAR.getFnr());
        Try<Arbeidsliste> resultUtenTittelEllerKommentarData = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_UTEN_KOMMENTAR_OG_TITTEL.getFnr());

        assertThat(resultUtenKommentarData.isSuccess()).isTrue();
        assertThat(resultUtenTittelData.isSuccess()).isTrue();
        assertThat(resultUtenTittelEllerKommentarData.isSuccess()).isTrue();
    }

    @Test
    void skalKunneOppdatereArbeidslisterUtenKommentar() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_3, jdbcTemplate);

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_3.getFnr());
        assertThat(TEST_ARBEIDSLISTE_3.kommentar).isEqualTo(result.get().getKommentar());

        Try<Arbeidsliste> updatedArbeidslisteUtenKommentar = result
                .map(arbeidsliste -> new ArbeidslisteDTO(TEST_ARBEIDSLISTE_3.fnr)
                        .setAktorId(TEST_ARBEIDSLISTE_3.getAktorId())
                        .setVeilederId(TEST_ARBEIDSLISTE_3.getVeilederId())
                        .setEndringstidspunkt(TEST_ARBEIDSLISTE_3.getEndringstidspunkt())
                        .setFrist(TEST_ARBEIDSLISTE_3.getFrist())
                        .setKommentar(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getFnr()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenKommentar.get().getKommentar()).isEqualTo(null);

    }

    @Test
    void skalKunneOppdatereArbeidslisterUtenTittel() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_3, jdbcTemplate);

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_3.getFnr());
        assertThat(TEST_ARBEIDSLISTE_3.overskrift).isEqualTo(result.get().getOverskrift());

        Try<Arbeidsliste> updatedArbeidslisteUtenTittel = result
                .map(arbeidsliste -> new ArbeidslisteDTO(TEST_ARBEIDSLISTE_3.fnr)
                        .setAktorId(TEST_ARBEIDSLISTE_3.getAktorId())
                        .setVeilederId(TEST_ARBEIDSLISTE_3.getVeilederId())
                        .setEndringstidspunkt(TEST_ARBEIDSLISTE_3.getEndringstidspunkt())
                        .setFrist(TEST_ARBEIDSLISTE_3.getFrist())
                        .setKommentar(TEST_ARBEIDSLISTE_3.getKommentar())
                        .setOverskrift(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getFnr()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenTittel.get().getOverskrift()).isEqualTo(null);
    }

    @Test
    void skalKunneOppdatereArbeidslisterUtenKommentarEllerTittel() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_3, jdbcTemplate);

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_3.getFnr());
        assertThat(TEST_ARBEIDSLISTE_3.kommentar).isEqualTo(result.get().getKommentar());
        assertThat(TEST_ARBEIDSLISTE_3.overskrift).isEqualTo(result.get().getOverskrift());

        Try<Arbeidsliste> updatedArbeidslisteUtenKommentarEllerTittel = result
                .map(arbeidsliste -> new ArbeidslisteDTO(TEST_ARBEIDSLISTE_3.fnr)
                        .setAktorId(TEST_ARBEIDSLISTE_3.getAktorId())
                        .setVeilederId(TEST_ARBEIDSLISTE_3.getVeilederId())
                        .setEndringstidspunkt(TEST_ARBEIDSLISTE_3.getEndringstidspunkt())
                        .setFrist(TEST_ARBEIDSLISTE_3.getFrist())
                        .setKommentar(null)
                        .setOverskrift(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getFnr()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenKommentarEllerTittel.get().getKommentar()).isEqualTo(null);
        assertThat(updatedArbeidslisteUtenKommentarEllerTittel.get().getOverskrift()).isEqualTo(null);
    }

    @Test
    void skalKunneOppdatereKategori() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(TEST_ARBEIDSLISTE_1.getFnr());
        assertThat(Arbeidsliste.Kategori.BLA).isEqualTo(result.get().getKategori());

        Try<Arbeidsliste> updatedArbeidsliste = result
                .map(arbeidsliste -> new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                        .setAktorId(TEST_ARBEIDSLISTE_1.getAktorId())
                        .setVeilederId(TEST_ARBEIDSLISTE_1.getVeilederId())
                        .setEndringstidspunkt(TEST_ARBEIDSLISTE_1.getEndringstidspunkt())
                        .setFrist(TEST_ARBEIDSLISTE_1.getFrist())
                        .setKommentar(TEST_ARBEIDSLISTE_1.getKommentar())
                        .setKategori(Arbeidsliste.Kategori.LILLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getFnr()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(Arbeidsliste.Kategori.LILLA).isEqualTo(updatedArbeidsliste.get().getKategori());
    }


    @Test
    void skalOppdatereEksisterendeArbeidsliste() {
        ArbeidslisteDTO arbeidsliste = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                .setAktorId(AktorId.of("22222222"))
                .setVeilederId(VeilederId.of("TEST_ID"))
                .setNavKontorForArbeidsliste("0000")
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Arbeidsliste 1 kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);
        insertArbeidsliste(arbeidsliste, jdbcTemplate);
        insertOppfolgingsInformasjon(arbeidsliste, jdbcTemplate);

        repo.updateArbeidsliste(arbeidsliste);
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(arbeidsliste.getFnr());

        assertThat(result.isSuccess()).isTrue();
        assertThat(VeilederId.of("TEST_ID")).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    void skalSletteEksisterendeArbeidsliste() {
        insertArbeidslister();

        final Integer rowsUpdated = repo.slettArbeidsliste(TEST_ARBEIDSLISTE_1.getAktorId(), Optional.of(TEST_ARBEIDSLISTE_1.getFnr()));

        assertThat(rowsUpdated).isEqualTo(1);
    }

    @Test
    void skalSletteEksisterendeArbeidslisteOgFargekategori() {
        insertArbeidslister();
        insertFargekategori(TEST_ARBEIDSLISTE_1, jdbcTemplate);

        final Integer rowsUpdated = repo.slettArbeidsliste(TEST_ARBEIDSLISTE_1.getAktorId(), Optional.of(TEST_ARBEIDSLISTE_1.getFnr()));

        assertThat(rowsUpdated).isEqualTo(2);
    }

    @Test
    void skalReturnereFailureVedFeil() {
        ArbeidslisteDTO arbeidsliste = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                .setAktorId(null)
                .setVeilederId(VeilederId.of("X11111"))
                .setNavKontorForArbeidsliste("0000")
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Arbeidsliste 1 kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        Try<ArbeidslisteDTO> result = repo.insertArbeidsliste(arbeidsliste);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void skalSletteArbeidslisteForAktoerids() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);

        AktorId aktoerId1 = TEST_ARBEIDSLISTE_1.getAktorId();
        Fnr fnr1 = TEST_ARBEIDSLISTE_1.getFnr();
        Try<Arbeidsliste> arbeidsliste = repo.retrieveArbeidsliste(fnr1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNotNull();

        final Integer rowsUpdated = repo.slettArbeidsliste(aktoerId1, Optional.empty());
        assertThat(rowsUpdated).isEqualTo(1);

        arbeidsliste = repo.retrieveArbeidsliste(fnr1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNull();
    }

    @Test
    void hentArbeidslisteForVeilederPaEnhet_filtrerPaEnhet() {
        EnhetId annetNavKontor = EnhetId.of("1111");
        ArbeidslisteDTO arbeidslistePaNyEnhet = new ArbeidslisteDTO(randomFnr())
                .setAktorId(randomAktorId())
                .setVeilederId(TEST_ARBEIDSLISTE_1.getVeilederId())
                .setFrist(TEST_ARBEIDSLISTE_1.getFrist())
                .setOverskrift(TEST_ARBEIDSLISTE_1.getOverskrift())
                .setKategori(TEST_ARBEIDSLISTE_1.getKategori())
                .setNavKontorForArbeidsliste(annetNavKontor.get())
                .setKommentar("Arbeidsliste 1 kopi kommentar");
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1.getAktorId(), TEST_ARBEIDSLISTE_1.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_2.getAktorId(), TEST_ARBEIDSLISTE_2.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);
        insertOppfolgingsInformasjon(arbeidslistePaNyEnhet.getAktorId(), arbeidslistePaNyEnhet.getVeilederId(), annetNavKontor, randomFnr(), jdbcTemplate);
        insertArbeidsliste(arbeidslistePaNyEnhet, jdbcTemplate);

        List<Arbeidsliste> arbeidslistes1 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), TEST_ARBEIDSLISTE_1.getVeilederId());
        List<Arbeidsliste> arbeidslistesAnnenEnhet = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(arbeidslistePaNyEnhet.getNavKontorForArbeidsliste()), arbeidslistePaNyEnhet.getVeilederId());

        assertThat(arbeidslistePaNyEnhet.getVeilederId()).isEqualTo(TEST_ARBEIDSLISTE_1.getVeilederId());
        assertThat(arbeidslistes1.size()).isEqualTo(1);
        assertThat(arbeidslistesAnnenEnhet.size()).isEqualTo(1);
        assertThat(arbeidslistes1.get(0).getKommentar()).isEqualTo(TEST_ARBEIDSLISTE_1.getKommentar());
        assertThat(arbeidslistesAnnenEnhet.get(0).getKommentar()).isEqualTo(arbeidslistePaNyEnhet.getKommentar());
    }

    @Test
    void hentArbeidslisteForVeilederPaEnhet_filtrerPaVeileder() {
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1.getAktorId(), TEST_ARBEIDSLISTE_1.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_2.getAktorId(), TEST_ARBEIDSLISTE_2.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);

        List<Arbeidsliste> arbeidslistes1 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), TEST_ARBEIDSLISTE_1.getVeilederId());
        List<Arbeidsliste> arbeidslistes2 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(TEST_ARBEIDSLISTE_2.getNavKontorForArbeidsliste()), TEST_ARBEIDSLISTE_2.getVeilederId());

        assertThat(arbeidslistes1.size()).isEqualTo(1);
        assertThat(arbeidslistes2.size()).isEqualTo(1);
        assertThat(arbeidslistes1.get(0).getKommentar()).isEqualTo(TEST_ARBEIDSLISTE_1.getKommentar());
        assertThat(arbeidslistes2.get(0).getKommentar()).isEqualTo(TEST_ARBEIDSLISTE_2.getKommentar());
    }

    @Test
    void hentArbeidslisteForVeilederPaEnhet_arbeidslisteKanLagesAvAnnenVeileder() {
        EnhetId navKontor = EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste());
        ArbeidslisteDTO arbeidslisteLagetAvAnnenVeileder = new ArbeidslisteDTO(randomFnr())
                .setAktorId(randomAktorId())
                .setVeilederId(randomVeilederId())
                .setFrist(TEST_ARBEIDSLISTE_1.getFrist())
                .setOverskrift(TEST_ARBEIDSLISTE_1.getOverskrift())
                .setKategori(TEST_ARBEIDSLISTE_1.getKategori())
                .setNavKontorForArbeidsliste(navKontor.get())
                .setKommentar("Arbeidsliste 1 kopi kommentar");
        insertArbeidslister();
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1.getAktorId(), TEST_ARBEIDSLISTE_1.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_2.getAktorId(), TEST_ARBEIDSLISTE_2.getVeilederId(), EnhetId.of(TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()), randomFnr(), jdbcTemplate);
        insertArbeidsliste(arbeidslisteLagetAvAnnenVeileder, jdbcTemplate);
        insertOppfolgingsInformasjon(arbeidslisteLagetAvAnnenVeileder.getAktorId(), TEST_ARBEIDSLISTE_1.getVeilederId(), navKontor, randomFnr(), jdbcTemplate);

        List<Arbeidsliste> arbeidslister = repo.hentArbeidslisteForVeilederPaEnhet(navKontor, TEST_ARBEIDSLISTE_1.getVeilederId());

        assertThat(arbeidslister.size()).isEqualTo(2);
        assertThat(arbeidslister.stream().anyMatch(x -> x.getKommentar().equals(TEST_ARBEIDSLISTE_1.getKommentar()))).isTrue();
        assertThat(arbeidslister.stream().anyMatch(x -> x.getKommentar().equals(arbeidslisteLagetAvAnnenVeileder.getKommentar()))).isTrue();
    }

    @Test
    void oppretting_av_arbeidsliste_skal_populere_tabeller_riktig_nar_bruker_ikke_har_arbeidsliste_fra_for() throws SQLException {
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);


        repo.insertArbeidsliste(TEST_ARBEIDSLISTE_1);

        Map<String, Object> arbeidslisteResultSet = jdbcTemplate.queryForMap("""
                SELECT * FROM arbeidsliste WHERE aktoerid = ?
                """, TEST_ARBEIDSLISTE_1.getAktorId().get());
        Map<String, Object> fargekategoriResultSet = jdbcTemplate.queryForMap("""
                SELECT * FROM fargekategori WHERE fnr = ?
                """, TEST_ARBEIDSLISTE_1.getFnr().get());

        // Sjekker bare på tilstedeværelse av verdier her da denne testen
        // er skrevet ifm. separering av FARGEKATEGORI ut fra ARBEIDSLISTE.
        // Det vi er interessert i her er med andre ord repositoryet
        // putter arbeidslisteinnhold og kategori i sine respektive tabeller.
        //
        // Testen kan tilpasses til å sjekke selve verdiene for de resterende kolonnene
        // om man ønsker det.
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.AKTOERID)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.SIST_ENDRET_AV_VEILEDERIDENT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.KOMMENTAR)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.FRIST)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.ENDRINGSTIDSPUNKT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.OVERSKRIFT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.ID)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.FNR)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.SIST_ENDRET)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.SIST_ENDRET_AV_VEILEDERIDENT)).isNotNull();

        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.KATEGORI)).isNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.VERDI)).isEqualTo("FARGEKATEGORI_A");
    }

    @Test
    void oppdatering_av_arbeidsliste_skal_populere_tabeller_riktig_nar_bruker_ikke_har_arbeidsliste_fra_for() throws SQLException {
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);
        String insertArbeidslisteQuery = """
                    INSERT INTO arbeidsliste (AKTOERID, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT,
                                OVERSKRIFT, KOMMENTAR, FRIST, KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE)
                                VALUES (?,?,?,?,?,?,?,?)
                """;
        jdbcTemplate.update(insertArbeidslisteQuery,
                TEST_ARBEIDSLISTE_1.getAktorId().get(),
                TEST_ARBEIDSLISTE_1.getVeilederId().getValue(),
                TEST_ARBEIDSLISTE_1.getEndringstidspunkt(),
                TEST_ARBEIDSLISTE_1.getOverskrift(),
                TEST_ARBEIDSLISTE_1.getKommentar(),
                TEST_ARBEIDSLISTE_1.getFrist(),
                null,
                TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()
        );

        repo.updateArbeidsliste(TEST_ARBEIDSLISTE_1_OPPDATERT);

        Map<String, Object> arbeidslisteResultSet = jdbcTemplate.queryForMap("""
                SELECT * FROM arbeidsliste WHERE aktoerid = ?
                """, TEST_ARBEIDSLISTE_1.getAktorId().get());
        Map<String, Object> fargekategoriResultSet = jdbcTemplate.queryForMap("""
                SELECT * FROM fargekategori WHERE fnr = ?
                """, TEST_ARBEIDSLISTE_1.getFnr().get());

        // Sjekker bare på tilstedeværelse av verdier her da denne testen
        // er skrevet ifm. separering av FARGEKATEGORI ut fra ARBEIDSLISTE.
        // Det vi er interessert i her er med andre ord repositoryet
        // oppdaterer arbeidslisteinnhold og kategori i sine respektive tabeller.
        //
        // Testen kan tilpasses til å sjekke selve verdiene for de resterende kolonnene
        // om man ønsker det.
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.AKTOERID)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.SIST_ENDRET_AV_VEILEDERIDENT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.KOMMENTAR)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.FRIST)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.ENDRINGSTIDSPUNKT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.OVERSKRIFT)).isNotNull();
        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.ID)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.FNR)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.SIST_ENDRET)).isNotNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.SIST_ENDRET_AV_VEILEDERIDENT)).isNotNull();

        assertThat(arbeidslisteResultSet.get(ARBEIDSLISTE.KATEGORI)).isNull();
        assertThat(fargekategoriResultSet.get(FARGEKATEGORI.VERDI)).isEqualTo("FARGEKATEGORI_B");
    }

    @Test
    void sletting_av_arbeidsliste_skal_populere_tabeller_riktig_nar_bruker_ikke_har_arbeidsliste_fra_for() throws SQLException {
        insertOppfolgingsInformasjon(TEST_ARBEIDSLISTE_1, jdbcTemplate);
        String insertArbeidslisteQuery = """
                    INSERT INTO arbeidsliste (AKTOERID, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT,
                                OVERSKRIFT, KOMMENTAR, FRIST, KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE)
                                VALUES (?,?,?,?,?,?,?,?)
                """;
        jdbcTemplate.update(insertArbeidslisteQuery,
                TEST_ARBEIDSLISTE_1.getAktorId().get(),
                TEST_ARBEIDSLISTE_1.getVeilederId().getValue(),
                TEST_ARBEIDSLISTE_1.getEndringstidspunkt(),
                TEST_ARBEIDSLISTE_1.getOverskrift(),
                TEST_ARBEIDSLISTE_1.getKommentar(),
                TEST_ARBEIDSLISTE_1.getFrist(),
                null,
                TEST_ARBEIDSLISTE_1.getNavKontorForArbeidsliste()
        );

        repo.slettArbeidsliste(TEST_ARBEIDSLISTE_1.getAktorId(), Optional.of(TEST_ARBEIDSLISTE_1.getFnr()));

        String aktorId = TEST_ARBEIDSLISTE_1.getAktorId().get();
        String fnr = TEST_ARBEIDSLISTE_1.getFnr().get();
        assertThrows(EmptyResultDataAccessException.class, () -> jdbcTemplate.queryForMap("""
                SELECT * FROM arbeidsliste WHERE aktoerid = ?
                """, aktorId));
        assertThrows(EmptyResultDataAccessException.class, () -> jdbcTemplate.queryForMap("""
                SELECT * FROM fargekategori WHERE fnr = ?
                """, fnr)
        );
    }

    private void insertArbeidslister() {
        insertArbeidsliste(TEST_ARBEIDSLISTE_1, jdbcTemplate);
        insertArbeidsliste(TEST_ARBEIDSLISTE_2, jdbcTemplate);
        insertArbeidsliste(TEST_ARBEIDSLISTE_3, jdbcTemplate);
        insertArbeidsliste(TEST_ARBEIDSLISTE_UTEN_TITTEL, jdbcTemplate);
        insertArbeidsliste(TEST_ARBEIDSLISTE_UTEN_KOMMENTAR, jdbcTemplate);
        insertArbeidsliste(TEST_ARBEIDSLISTE_UTEN_KOMMENTAR_OG_TITTEL, jdbcTemplate);
    }

    public static void insertArbeidsliste(ArbeidslisteDTO arbeidslisteDTO, JdbcTemplate jdbcTemplate) {
        String insertArbeidslisteQuery = """
                    INSERT INTO arbeidsliste (AKTOERID, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT,
                                OVERSKRIFT, KOMMENTAR, FRIST, KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE)
                                VALUES (?,?,?,?,?,?,?,?)
                """;
        jdbcTemplate.update(insertArbeidslisteQuery,
                arbeidslisteDTO.getAktorId().get(),
                arbeidslisteDTO.getVeilederId().getValue(),
                arbeidslisteDTO.getEndringstidspunkt(),
                arbeidslisteDTO.getOverskrift(),
                arbeidslisteDTO.getKommentar(),
                arbeidslisteDTO.getFrist(),
                Optional.ofNullable(arbeidslisteDTO.getKategori()).map((Enum::toString)).orElse(null),
                arbeidslisteDTO.getNavKontorForArbeidsliste()
        );
    }

    public static void insertFargekategori(ArbeidslisteDTO arbeidslisteDTO, JdbcTemplate jdbcTemplate) {
        String insertFargekategoriQuery = """
                INSERT INTO fargekategori (ID, FNR, VERDI, SIST_ENDRET, SIST_ENDRET_AV_VEILEDERIDENT)
                                VALUES (?,?,?,?,?)
                """;
        jdbcTemplate.update(insertFargekategoriQuery,
                UUID.randomUUID(),
                arbeidslisteDTO.getFnr().get(),
                ArbeidslisteMapper.mapTilFargekategoriVerdi(arbeidslisteDTO.getKategori()),
                Timestamp.valueOf(LocalDateTime.now()),
                arbeidslisteDTO.getVeilederId().toString()
        );
    }

    public static void insertOppfolgingsInformasjon(ArbeidslisteDTO arbeidslisteDTO, JdbcTemplate jdbcTemplate) {
        int person = current().nextInt();
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, arbeidslisteDTO.getAktorId().get(), PDLIdent.Gruppe.AKTORID.name());
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, arbeidslisteDTO.getFnr().get(), PDLIdent.Gruppe.FOLKEREGISTERIDENT.name());
        jdbcTemplate.update("INSERT INTO oppfolgingsbruker_arena_v2 (fodselsnr, nav_kontor) values (?,?)", arbeidslisteDTO.getFnr().get(), arbeidslisteDTO.getNavKontorForArbeidsliste());
        jdbcTemplate.update("""
                INSERT INTO oppfolging_data (AKTOERID, OPPFOLGING, STARTDATO) VALUES (?,?,?)
                ON CONFLICT (AKTOERID) DO UPDATE SET OPPFOLGING = EXCLUDED.OPPFOLGING, STARTDATO = EXCLUDED.STARTDATO
                """, arbeidslisteDTO.getAktorId().get(), true, toTimestamp(ZonedDateTime.now()));
        jdbcTemplate.update("UPDATE oppfolging_data SET veilederid = ? WHERE aktoerid = ?", arbeidslisteDTO.getVeilederId().getValue(), arbeidslisteDTO.getAktorId().get());
    }

    private static void insertOppfolgingsInformasjon(AktorId aktorId, VeilederId veilederId, EnhetId navKontor, Fnr fnr, JdbcTemplate jdbcTemplate) {
        int person = current().nextInt();
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, aktorId.get(), PDLIdent.Gruppe.AKTORID.name());
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, fnr.get(), PDLIdent.Gruppe.FOLKEREGISTERIDENT.name());
        jdbcTemplate.update("INSERT INTO oppfolgingsbruker_arena_v2 (fodselsnr, nav_kontor) values (?,?)", fnr.get(), navKontor.get());
        jdbcTemplate.update("""
                INSERT INTO oppfolging_data (AKTOERID, OPPFOLGING, STARTDATO) VALUES (?,?,?)
                ON CONFLICT (AKTOERID) DO UPDATE SET OPPFOLGING = EXCLUDED.OPPFOLGING, STARTDATO = EXCLUDED.STARTDATO
                """, aktorId.get(), true, toTimestamp(ZonedDateTime.now()));
        jdbcTemplate.update("UPDATE oppfolging_data SET veilederid = ? WHERE aktoerid = ?", veilederId.getValue(), aktorId.get());
    }
}
