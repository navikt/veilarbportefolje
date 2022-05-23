package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static java.util.concurrent.ThreadLocalRandom.current;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArbeidslisteRepositoryTestV2 {
    @Autowired
    private ArbeidslisteRepositoryV2 repo;
    @Autowired
    @Qualifier("PostgresJdbc")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepository;

    private final ArbeidslisteDTO data = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
            .setAktorId(AktorId.of("22222222"))
            .setVeilederId(VeilederId.of("X11111"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 1 kommentar")
            .setOverskrift("Dette er en overskrift")
            .setKategori(Arbeidsliste.Kategori.BLA);
    private final ArbeidslisteDTO data2 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101011"))
            .setAktorId(AktorId.of("22222223"))
            .setVeilederId(VeilederId.of("X11112"))
            .setNavKontorForArbeidsliste("0000")
            .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
            .setKommentar("Arbeidsliste 2 kommentar")
            .setKategori(Arbeidsliste.Kategori.GRONN);

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE ARBEIDSLISTE");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }

    @Test
    public void skalKunneHenteArbeidsliste() {
        insertArbeidslister();
        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());
        assertThat(result.isSuccess()).isTrue();
        assertThat(data.getVeilederId()).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    public void skalKunneOppdatereKategori() {
        insertArbeidslister();

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());
        assertThat(Arbeidsliste.Kategori.BLA).isEqualTo(result.get().getKategori());

        Try<Arbeidsliste> updatedArbeidsliste = result
                .map(arbeidsliste -> new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                        .setAktorId(data.getAktorId())
                        .setVeilederId(data.getVeilederId())
                        .setEndringstidspunkt(data.getEndringstidspunkt())
                        .setFrist(data.getFrist())
                        .setKommentar(data.getKommentar())
                        .setKategori(Arbeidsliste.Kategori.LILLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getAktorId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(Arbeidsliste.Kategori.LILLA).isEqualTo(updatedArbeidsliste.get().getKategori());
    }


    @Test
    public void skalOppdatereEksisterendeArbeidsliste() {
        insertArbeidslister();

        VeilederId expected = VeilederId.of("TEST_ID");
        repo.updateArbeidsliste(data.setVeilederId(expected));

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data.getAktorId());

        assertThat(result.isSuccess()).isTrue();
        assertThat(expected).isEqualTo(result.get().getSistEndretAv());
    }

    @Test
    public void skalSletteEksisterendeArbeidsliste() {
        insertArbeidslister();
        final Integer rowsUpdated = repo.slettArbeidsliste(data.getAktorId());
        assertThat(rowsUpdated).isEqualTo(1);
    }

    @Test
    public void skalReturnereFailureVedFeil() {
        Try<ArbeidslisteDTO> result = repo.insertArbeidsliste(data.setAktorId(null));
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    public void skalSletteArbeidslisteForAktoerids() {
        insertArbeidslister();

        AktorId aktoerId1 = AktorId.of("22222222");
        Try<Arbeidsliste> arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNotNull();

        final Integer rowsUpdated = repo.slettArbeidsliste(aktoerId1);
        assertThat(rowsUpdated).isEqualTo(1);

        arbeidsliste = repo.retrieveArbeidsliste(aktoerId1);
        assertThat(arbeidsliste.isSuccess()).isTrue();
        assertThat(arbeidsliste.get()).isNull();
    }

    @Test
    public void hentArbeidslisteForVeilederPaEnhet_filtrerPaEnhet() {
        EnhetId annetNavKontor = EnhetId.of("1111");
        ArbeidslisteDTO arbeidslistePaNyEnhet = new ArbeidslisteDTO(randomFnr())
                .setAktorId(randomAktorId())
                .setVeilederId(data.getVeilederId())
                .setFrist(data.getFrist())
                .setOverskrift(data.getOverskrift())
                .setKategori(data.getKategori())
                .setNavKontorForArbeidsliste(annetNavKontor.get())
                .setKommentar("Arbeidsliste 1 kopi kommentar");

        insertArbeidslister();
        insertOppfolgingsInformasjon();
        insertOppfolgingsInformasjon(arbeidslistePaNyEnhet.getAktorId(), arbeidslistePaNyEnhet.getVeilederId(), annetNavKontor);
        repo.insertArbeidsliste(arbeidslistePaNyEnhet);

        List<Arbeidsliste> arbeidslistes1 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(data.getNavKontorForArbeidsliste()), data.getVeilederId());
        List<Arbeidsliste> arbeidslistesAnnenEnhet = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(arbeidslistePaNyEnhet.getNavKontorForArbeidsliste()), arbeidslistePaNyEnhet.getVeilederId());

        assertThat(arbeidslistePaNyEnhet.getVeilederId()).isEqualTo(data.getVeilederId());

        assertThat(arbeidslistes1.size()).isEqualTo(1);
        assertThat(arbeidslistesAnnenEnhet.size()).isEqualTo(1);
        assertThat(arbeidslistes1.get(0).getKommentar()).isEqualTo(data.getKommentar());
        assertThat(arbeidslistesAnnenEnhet.get(0).getKommentar()).isEqualTo(arbeidslistePaNyEnhet.getKommentar());
    }

    @Test
    public void hentArbeidslisteForVeilederPaEnhet_filtrerPaVeileder() {
        insertArbeidslister();
        insertOppfolgingsInformasjon();
        List<Arbeidsliste> arbeidslistes1 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(data.getNavKontorForArbeidsliste()), data.getVeilederId());
        List<Arbeidsliste> arbeidslistes2 = repo.hentArbeidslisteForVeilederPaEnhet(EnhetId.of(data2.getNavKontorForArbeidsliste()), data2.getVeilederId());

        assertThat(arbeidslistes1.size()).isEqualTo(1);
        assertThat(arbeidslistes2.size()).isEqualTo(1);
        assertThat(arbeidslistes1.get(0).getKommentar()).isEqualTo(data.getKommentar());
        assertThat(arbeidslistes2.get(0).getKommentar()).isEqualTo(data2.getKommentar());
    }

    @Test
    public void hentArbeidslisteForVeilederPaEnhet_arbeidslisteKanLagesAvAnnenVeileder() {
        EnhetId navKontor = EnhetId.of(data.getNavKontorForArbeidsliste());
        ArbeidslisteDTO arbeidslisteLagetAvAnnenVeileder = new ArbeidslisteDTO(randomFnr())
                .setAktorId(randomAktorId())
                .setVeilederId(randomVeilederId())
                .setFrist(data.getFrist())
                .setOverskrift(data.getOverskrift())
                .setKategori(data.getKategori())
                .setNavKontorForArbeidsliste(navKontor.get())
                .setKommentar("Arbeidsliste 1 kopi kommentar");
        insertArbeidslister();
        insertOppfolgingsInformasjon();
        repo.insertArbeidsliste(arbeidslisteLagetAvAnnenVeileder);
        insertOppfolgingsInformasjon(arbeidslisteLagetAvAnnenVeileder.getAktorId(), data.getVeilederId(), navKontor);

        List<Arbeidsliste> arbeidslister = repo.hentArbeidslisteForVeilederPaEnhet(navKontor, data.getVeilederId());

        assertThat(arbeidslister.size()).isEqualTo(2);
        assertThat(arbeidslister.stream().anyMatch(x -> x.getKommentar().equals(data.getKommentar()))).isTrue();
        assertThat(arbeidslister.stream().anyMatch(x -> x.getKommentar().equals(arbeidslisteLagetAvAnnenVeileder.getKommentar()))).isTrue();
    }

    private void insertArbeidslister() {
        Try<ArbeidslisteDTO> result1 = repo.insertArbeidsliste(data);
        Try<ArbeidslisteDTO> result2 = repo.insertArbeidsliste(data2);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
    }

    private void insertOppfolgingsInformasjon() {
        insertOppfolgingsInformasjon(data.getAktorId(), data.getVeilederId(), EnhetId.of(data.getNavKontorForArbeidsliste()));
        insertOppfolgingsInformasjon(data2.getAktorId(), data2.getVeilederId(), EnhetId.of(data.getNavKontorForArbeidsliste()));
    }

    private void insertOppfolgingsInformasjon(AktorId aktorId, VeilederId veilederId, EnhetId navKontor) {
        int person = current().nextInt();
        Fnr fnr = randomFnr();
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, aktorId.get(), PDLIdent.Gruppe.AKTORID.name());
        jdbcTemplate.update("INSERT INTO bruker_identer (person, ident, gruppe, historisk) values (?,?,?, false)",
                person, fnr.get(), PDLIdent.Gruppe.FOLKEREGISTERIDENT.name());
        jdbcTemplate.update("INSERT INTO oppfolgingsbruker_arena_v2 (fodselsnr, nav_kontor) values (?,?)", fnr.get(), navKontor.get());
        oppfolgingRepository.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolgingRepository.settVeileder(aktorId, veilederId);
    }
}
