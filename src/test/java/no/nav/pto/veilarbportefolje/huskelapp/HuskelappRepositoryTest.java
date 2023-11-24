package no.nav.pto.veilarbportefolje.huskelapp;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class HuskelappRepositoryTest {
    @Autowired
    private HuskelappRepository repo;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepository;


    Fnr fnr1 = Fnr.ofValidFnr("01010101010");
    Timestamp frist1 = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));


    private final HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr1,
            frist1, ("Huskelapp nr.1 sin kommentar"), EnhetId.of("0010"));

    private final HuskelappOpprettRequest huskelapp2 = new HuskelappOpprettRequest(Fnr.ofValidFnr("01010101011"),
            (Timestamp.from(Instant.parse("2017-10-11T00:00:00Z"))), ("Huskelapp nr.2 sin kommentar"), EnhetId.of("0010"));

    private final HuskelappOpprettRequest huskelapp3 = new HuskelappOpprettRequest(Fnr.ofValidFnr("01010101015"),
            (Timestamp.from(Instant.parse("2017-10-11T00:00:00Z"))), ("Huskelapp nr.3 sin kommentar"), EnhetId.of("0010"));

    private final HuskelappOpprettRequest huskelapp4 = new HuskelappOpprettRequest(Fnr.ofValidFnr("01010101012"),
            (Timestamp.from(Instant.parse("2017-10-11T00:00:00Z"))), ("Huskelapp nr.4 sin kommentar"), EnhetId.of("0010"));

    private final HuskelappOpprettRequest huskelappUtenKommentar = new HuskelappOpprettRequest(Fnr.ofValidFnr("01010101013"),
            (Timestamp.from(Instant.parse("2024-10-11T00:00:00Z"))), (null), EnhetId.of("0010"));

    private final HuskelappOpprettRequest huskelapp6 = new HuskelappOpprettRequest(Fnr.ofValidFnr("01010101014"),
            (Timestamp.from(Instant.parse("2017-10-11T00:00:00Z"))), (null), EnhetId.of("0010"));

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE HUSKELAPP");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }


    @Test
    public void skalKunneOppretteOgHenteHuskelapp() {
        repo.opprettHuskelapp(huskelapp1, VeilederId.of("Z123456"));
        Optional<HuskelappOutputDto> result = repo.hentAktivHuskelapp(fnr1);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().enhetID().toString()).isEqualTo("0010");
        assertThat(result.get().frist()).isEqualTo(frist1);
    }


    @Test
    public void skalKunneHenteHuskelappUtenKommentar() {
        repo.opprettHuskelapp(huskelappUtenKommentar, VeilederId.of("Z123456"));
        Optional<HuskelappOutputDto> result = repo.hentAktivHuskelapp(Fnr.ofValidFnr("01010101013"));
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().kommentar()).isEqualTo(null);
    }


    @Test
    public void skalKunneRedigereHuskelapp() {
        Fnr fnrBruker = Fnr.ofValidFnr("01010101011");
        Timestamp nyFrist = Timestamp.from(Instant.parse("2025-10-11T00:00:00Z"));
        repo.opprettHuskelapp(huskelapp2, VeilederId.of("Z123456"));
        Optional<HuskelappOutputDto> huskelappFør = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelappFør.isPresent()).isTrue();
        assertThat(huskelappFør.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappFør.get().huskelappId(), fnrBruker, nyFrist, "ny kommentar på huskelapp nr.2", EnhetId.of("0010"));
        repo.redigerHuskelapp(huskelappRedigerRequest, VeilederId.of("Z234567"));
        Optional<HuskelappOutputDto> huskelappEtter = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelappEtter.isPresent()).isTrue();
        assertThat(huskelappEtter.get().kommentar()).isEqualTo("ny kommentar på huskelapp nr.2");
        assertThat(huskelappEtter.get().frist()).isEqualTo(nyFrist);
    }

    @Test
    public void skalKunneSletteHuskelapp() {
        Fnr fnrBruker = Fnr.ofValidFnr("01010101011");
        repo.opprettHuskelapp(huskelapp2, VeilederId.of("Z123456"));
        Optional<HuskelappOutputDto> huskelapp = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelapp.isPresent()).isTrue();
        assertThat(huskelapp.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");
        repo.settSisteHuskelappRadIkkeAktiv(huskelapp.get().huskelappId());
        Optional<HuskelappOutputDto> huskelappEtter = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelappEtter.isPresent()).isFalse();
    }

    @Test
    public void skalKunneSletteHuskelappMedFlereRader() {
        Fnr fnrBruker = Fnr.ofValidFnr("01010101011");
        repo.opprettHuskelapp(huskelapp2, VeilederId.of("Z123456"));
        Optional<HuskelappOutputDto> huskelappFoer = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelappFoer.isPresent()).isTrue();
        assertThat(huskelappFoer.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappFoer.get().huskelappId(), fnrBruker, huskelappFoer.get().frist(), "ny kommentar på huskelapp nr.2", EnhetId.of("0010"));
        repo.redigerHuskelapp(huskelappRedigerRequest, VeilederId.of("Z123456"));
        List<HuskelappOutputDto> alleHuskelappRader = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId());
        repo.settSisteHuskelappRadIkkeAktivOld(huskelappFoer.get().huskelappId());
        Optional<HuskelappOutputDto> huskelappEtter = repo.hentAktivHuskelapp(fnrBruker);
        assertThat(huskelappEtter.isPresent()).isFalse();
    }


    //Slett huskelapp av annen veileder -sjekk at det ikke er ok
    //Bør frist ikke ha lov til å være bakover i tid, eller?


    /*
    @Test
    public void skalKunneOppdatereArbeidslisterUtenKommentar() {
        insertArbeidslister();

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data3.getAktorId());
        assertThat(data3.kommentar).isEqualTo(result.get().getKommentar());

        Try<Arbeidsliste> updatedArbeidslisteUtenKommentar = result
                .map(arbeidsliste -> new ArbeidslisteDTO(data3.fnr)
                        .setAktorId(data3.getAktorId())
                        .setVeilederId(data3.getVeilederId())
                        .setEndringstidspunkt(data3.getEndringstidspunkt())
                        .setFrist(data3.getFrist())
                        .setKommentar(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getAktorId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenKommentar.get().getKommentar()).isEqualTo(null);

    }

    @Test
    public void skalKunneOppdatereArbeidslisterUtenTittel() {
        insertArbeidslister();

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data3.getAktorId());
        assertThat(data3.overskrift).isEqualTo(result.get().getOverskrift());

        Try<Arbeidsliste> updatedArbeidslisteUtenTittel = result
                .map(arbeidsliste -> new ArbeidslisteDTO(data3.fnr)
                        .setAktorId(data3.getAktorId())
                        .setVeilederId(data3.getVeilederId())
                        .setEndringstidspunkt(data3.getEndringstidspunkt())
                        .setFrist(data3.getFrist())
                        .setKommentar(data3.getKommentar())
                        .setOverskrift(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getAktorId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenTittel.get().getOverskrift()).isEqualTo(null);
    }

    @Test
    public void skalKunneOppdatereArbeidslisterUtenKommentarEllerTittel() {
        insertArbeidslister();

        Try<Arbeidsliste> result = repo.retrieveArbeidsliste(data3.getAktorId());
        assertThat(data3.kommentar).isEqualTo(result.get().getKommentar());
        assertThat(data3.overskrift).isEqualTo(result.get().getOverskrift());

        Try<Arbeidsliste> updatedArbeidslisteUtenKommentarEllerTittel = result
                .map(arbeidsliste -> new ArbeidslisteDTO(data3.fnr)
                        .setAktorId(data3.getAktorId())
                        .setVeilederId(data3.getVeilederId())
                        .setEndringstidspunkt(data3.getEndringstidspunkt())
                        .setFrist(data3.getFrist())
                        .setKommentar(null)
                        .setOverskrift(null)
                        .setKategori(Arbeidsliste.Kategori.BLA))
                .flatMap(oppdatertArbeidsliste -> repo.updateArbeidsliste(oppdatertArbeidsliste))
                .flatMap(arbeidslisteDTO -> repo.retrieveArbeidsliste(arbeidslisteDTO.getAktorId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(updatedArbeidslisteUtenKommentarEllerTittel.get().getKommentar()).isEqualTo(null);
        assertThat(updatedArbeidslisteUtenKommentarEllerTittel.get().getOverskrift()).isEqualTo(null);
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

    private void insertHuskelapper() {
        Try<ArbeidslisteDTO> result1 = repo.insertArbeidsliste(data);
        Try<ArbeidslisteDTO> result2 = repo.insertArbeidsliste(data2);
        Try<ArbeidslisteDTO> result3 = repo.insertArbeidsliste(data3);
        Try<ArbeidslisteDTO> resultUtenTittelData = repo.insertArbeidsliste(utenTittelData);
        Try<ArbeidslisteDTO> resultUtenKommentarData = repo.insertArbeidsliste(utenKommentarData);
        Try<ArbeidslisteDTO> resultUtenTittelEllerKommentarData = repo.insertArbeidsliste(utenTittelellerKommentarData);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result3.isSuccess()).isTrue();
        assertThat(resultUtenTittelData.isSuccess()).isTrue();
        assertThat(resultUtenKommentarData.isSuccess()).isTrue();
        assertThat(resultUtenTittelEllerKommentarData.isSuccess()).isTrue();
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
    */
}
