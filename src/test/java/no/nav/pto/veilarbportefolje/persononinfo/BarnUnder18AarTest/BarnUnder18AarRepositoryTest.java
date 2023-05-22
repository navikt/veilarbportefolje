package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18Aar;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.domene.Kjonn.M;
import static no.nav.pto.veilarbportefolje.util.DateUtils.alderFraFodselsdato;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class BarnUnder18AarRepositoryTest {

    @Autowired
    private BarnUnder18AarRepository barnUnder18AarRepository;

    @Autowired
    private BarnUnder18AarService barnUnder18AarService;

    @Autowired
    private PdlPersonRepository pdlPersonRepository;

    @BeforeEach
    public void setUp() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        final UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(anyString())).thenReturn(true);
        this.barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        this.barnUnder18AarService = new BarnUnder18AarService(new BarnUnder18AarRepository(db, db));
        this.pdlPersonRepository = new PdlPersonRepository(db, null);

        db.update("TRUNCATE foreldreansvar");
        db.update("TRUNCATE bruker_data_barn");
    }


    @Test
    public void sjekkAtBarnBlirRiktigInsertedITabellen() {
        Fnr fnrPerson = randomFnr();
        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        List<BarnUnder18Aar> barnFraPdl = List.of(pdlBarn15Aar());
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, barnFraPdl);
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnrPerson));
        Assertions.assertFalse(forelderBarnMap.isEmpty());
        Assertions.assertTrue(forelderBarnMap.get(fnrPerson).get(0).getAlder().equals(15));
    }

    @Test
    public void barnBlirInsertedPaToUlikeForeldre() {
        Fnr foresatt1 = randomFnr();
        Fnr foresatt2 = randomFnr();
        pdlPersonRepository.upsertPerson(foresatt1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlPersonRepository.upsertPerson(foresatt2, new PDLPerson().setKjonn(M).setFoedsel(LocalDate.now()));
        List<BarnUnder18Aar> barnFraPdl = List.of(pdlBarn());
        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt1, barnFraPdl);
        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt2, barnFraPdl);
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18Aar(List.of(foresatt1, foresatt2));
        Assertions.assertTrue(forelderBarnMap.size() == 2);
        Assertions.assertTrue(forelderBarnMap.get(foresatt1).equals(forelderBarnMap.get(foresatt2)));
    }


    @Test
    public void testOppdaterEndringPaBarn() {
        Fnr barnFnr = randomFnr();
        PDLPersonBarn pdlPersonBarn = pdlPersonBarn();
        barnUnder18AarService.oppdaterEndringPaBarn(barnFnr, pdlPersonBarn);

        BarnUnder18AarData barnUnder18AarData = barnUnder18AarRepository.hentInfoOmBarn(barnFnr);
        Assertions.assertEquals(barnUnder18AarData.getDiskresjonskode(), pdlPersonBarn.getDiskresjonskode());
        Assertions.assertEquals(barnUnder18AarData.getAlder(), alderFraFodselsdato(pdlPersonBarn.getFodselsdato()));
    }

    @Test
    public void testErFnrBarnAvForelderUnderOppfolging() {
        Fnr fnrPerson = randomFnr();
        List<BarnUnder18Aar> barnFraPdl = List.of(pdlBarn15Aar());
        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, barnFraPdl);

        boolean erFnrBarnAvForelderUnderOppfolging = barnUnder18AarService.erFnrBarnAvForelderUnderOppfolging(List.of(barnFraPdl.get(0).getFnr()));
        Assertions.assertTrue(erFnrBarnAvForelderUnderOppfolging);
    }

    @Test
    public void testSlettBarnDataHvisIngenForeldreErUnderOppfolging() {
        BarnUnder18Aar barnUnder18Aar = pdlBarn();
        barnUnder18AarRepository.lagreBarnData(barnUnder18Aar.getFnr(), barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());

        BarnUnder18AarData lagretDataForBarn = barnUnder18AarRepository.hentInfoOmBarn(pdlBarn().getFnr());
        Assertions.assertNotNull(lagretDataForBarn);
        Assertions.assertEquals(lagretDataForBarn.getAlder(), DateUtils.alderFraFodselsdato(barnUnder18Aar.getFodselsdato()));

        barnUnder18AarService.slettBarnDataHvisIngenForeldreErUnderOppfolging(List.of(barnUnder18Aar.getFnr()));

        lagretDataForBarn = barnUnder18AarRepository.hentInfoOmBarn(pdlBarn().getFnr());
        Assertions.assertNull(lagretDataForBarn);

    }

    private BarnUnder18Aar pdlBarn() {
        return new BarnUnder18Aar()
                .setFnr(Fnr.of("12312312312"))
                .setFodselsdato(LocalDate.of(2004, 12, 12))
                .setDiskresjonskode("6");
    }

    private BarnUnder18Aar pdlBarn15Aar() {
        LocalDate iDag = LocalDate.now();
        LocalDate fodselsdato15Aar = iDag.minusYears(15).minusMonths(1);
        return new BarnUnder18Aar()
                .setFnr(Fnr.of("12312312312"))
                .setFodselsdato(fodselsdato15Aar)
                .setDiskresjonskode("6");
    }

    private PDLPersonBarn pdlPersonBarn() {
        LocalDate iDag = LocalDate.now();
        LocalDate fodselsdato15Aar = iDag.minusYears(15).minusMonths(1);
        return new PDLPersonBarn()
                .setFodselsdato(fodselsdato15Aar)
                .setDiskresjonskode("6")
                .setErIlive(true);
    }

}
