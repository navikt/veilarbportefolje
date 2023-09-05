package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.service.DefaultUnleash;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.domene.Kjonn.M;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.DateUtils.alderFraFodselsdato;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
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

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    @Autowired
    private PdlPortefoljeClient mockedPdlPortefoljeClient;

    @BeforeEach
    public void setUp() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        final DefaultUnleash unleashService = mock(DefaultUnleash.class);
        when(unleashService.isEnabled(anyString())).thenReturn(true);
        this.barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        this.barnUnder18AarService = new BarnUnder18AarService(barnUnder18AarRepository);
        this.pdlPersonRepository = new PdlPersonRepository(db, null);

        db.update("TRUNCATE bruker_identer");
        db.update("TRUNCATE foreldreansvar CASCADE ");
        db.update("TRUNCATE bruker_data_barn CASCADE");
    }


    @Test
    public void sjekkAtBarnBlirRiktigInsertedITabellen() {
        Fnr fnrPerson = randomFnr();
        Fnr fnrBarn = randomFnr();
        List<PDLIdent> identerBruker = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(fnrPerson.get(), false, FOLKEREGISTERIDENT)
        );

        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identerBruker);
        Mockito.when(mockedPdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(fnrBarn))).thenReturn(Map.of(fnrBarn, pdlBarn15Aar()));

        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, Map.of(fnrBarn, pdlBarn15Aar()), Collections.emptyList());
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnrPerson));
        Assertions.assertFalse(forelderBarnMap.isEmpty());
        Assertions.assertEquals(15, (int) forelderBarnMap.get(fnrPerson).get(0).getAlder());
    }

    @Test
    public void barnBlirInsertedPaToUlikeForeldre() {
        Fnr foresatt1 = randomFnr();
        Fnr foresatt2 = randomFnr();
        Fnr fnrBarn = randomFnr();

        pdlPersonRepository.upsertPerson(foresatt1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlPersonRepository.upsertPerson(foresatt2, new PDLPerson().setKjonn(M).setFoedsel(LocalDate.now()));
        Mockito.when(mockedPdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(fnrBarn))).thenReturn(Map.of(fnrBarn, pdlBarn()));

        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt1, Map.of(fnrBarn, pdlBarn()), Collections.emptyList());
        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt2, Map.of(fnrBarn, pdlBarn()), Collections.emptyList());
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18Aar(List.of(foresatt1, foresatt2));
        Assertions.assertEquals(2, forelderBarnMap.size());
        Assertions.assertEquals(forelderBarnMap.get(foresatt1), forelderBarnMap.get(foresatt2));
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
        Fnr fnrBarn = randomFnr();
        List<PDLIdent> identerBruker = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(fnrPerson.get(), false, FOLKEREGISTERIDENT)
        );


        Mockito.when(mockedPdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(fnrBarn))).thenReturn(Map.of(fnrBarn, pdlBarn15Aar()));
        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identerBruker);
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, Map.of(fnrBarn, pdlBarn15Aar()), Collections.emptyList());

        boolean erFnrBarnAvForelderUnderOppfolging = barnUnder18AarService.erFnrBarnAvForelderUnderOppfolging(List.of(fnrBarn));
        Assertions.assertTrue(erFnrBarnAvForelderUnderOppfolging);
    }

    @Test
    public void testSlettBarnDataHvisIngenForeldreErUnderOppfolging() {
        PDLPersonBarn barnOver18Aar = pdlBarn18Aar();
        PDLPersonBarn barnUnder18Aar = pdlBarn15Aar();
        Fnr fnrBarnOver18Aar = randomFnr();
        Fnr fnrBarnUnder18Aar = randomFnr();

        barnUnder18AarRepository.lagreBarnData(fnrBarnOver18Aar, barnOver18Aar.getFodselsdato(), barnOver18Aar.getDiskresjonskode());
        barnUnder18AarRepository.lagreBarnData(fnrBarnUnder18Aar, barnUnder18Aar.getFodselsdato(), barnUnder18Aar.getDiskresjonskode());

        BarnUnder18AarData lagretDataForBarn18Ar = barnUnder18AarRepository.hentInfoOmBarn(fnrBarnOver18Aar);
        BarnUnder18AarData lagretDataForBarn15Ar = barnUnder18AarRepository.hentInfoOmBarn(fnrBarnUnder18Aar);
        Assertions.assertNotNull(lagretDataForBarn15Ar);
        Assertions.assertNotNull(lagretDataForBarn18Ar);
        Assertions.assertEquals(lagretDataForBarn15Ar.getAlder(), alderFraFodselsdato(barnUnder18Aar.getFodselsdato()));
        Assertions.assertEquals(lagretDataForBarn18Ar.getAlder(), alderFraFodselsdato(barnOver18Aar.getFodselsdato()));

        barnUnder18AarService.slettDataForBarnSomErOver18();

        lagretDataForBarn18Ar = barnUnder18AarRepository.hentInfoOmBarn(fnrBarnOver18Aar);
        lagretDataForBarn15Ar = barnUnder18AarRepository.hentInfoOmBarn(fnrBarnUnder18Aar);
        Assertions.assertNull(lagretDataForBarn18Ar);
        Assertions.assertNotNull(lagretDataForBarn15Ar);
    }

    @Test
    public void testOppdateringAvBarnIdent() {
        Fnr fnrPerson = randomFnr();
        Fnr fnrBarn = randomFnr();
        List<PDLIdent> identerBruker = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(fnrPerson.get(), false, FOLKEREGISTERIDENT)
        );

        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identerBruker);
        Mockito.when(mockedPdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(fnrBarn))).thenReturn(Map.of(fnrBarn, pdlBarn15Aar()));
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, Map.of(fnrBarn, pdlBarn15Aar()), Collections.emptyList());

        Fnr nyFnr = randomFnr();
        barnUnder18AarRepository.oppdatereBarnIdent(nyFnr, List.of(fnrBarn, randomFnr()));
        BarnUnder18AarData barnMedNyIdent = barnUnder18AarRepository.hentInfoOmBarn(nyFnr);
        Boolean finnesNyIdentIForeldreansvar = barnUnder18AarRepository.finnesBarnIForeldreansvar(nyFnr);
        Boolean finnesGamelIdentIForeldreansvar = barnUnder18AarRepository.finnesBarnIForeldreansvar(fnrBarn);

        Assertions.assertNotNull(barnMedNyIdent);
        Assertions.assertEquals(barnMedNyIdent.getAlder(), alderFraFodselsdato(pdlBarn15Aar().getFodselsdato()));
        Assertions.assertEquals(barnMedNyIdent.getDiskresjonskode(), pdlBarn15Aar().getDiskresjonskode());
        Assertions.assertTrue(finnesNyIdentIForeldreansvar);
        Assertions.assertFalse(finnesGamelIdentIForeldreansvar);
    }

    @Test
    public void testFjerningAvData(){
        Fnr fnrPerson = randomFnr();
        Fnr fnrBarn1 = randomFnr();
        Fnr fnrBarn2 = randomFnr();

        List<PDLIdent> identerBruker = List.of(
                new PDLIdent(randomAktorId().get(), false, AKTORID),
                new PDLIdent(fnrPerson.get(), false, FOLKEREGISTERIDENT)
        );

        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identerBruker);
        Mockito.when(mockedPdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(fnrBarn1, fnrBarn2))).thenReturn(Map.of(fnrBarn1, pdlBarn15Aar(), fnrBarn2, pdlBarn4Aar()));
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, Map.of(fnrBarn1, pdlBarn15Aar(), fnrBarn2, pdlBarn4Aar()), Collections.emptyList());

        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, Map.of(fnrBarn2, pdlBarn4Aar()), Collections.emptyList());
        List<Fnr> barnFnrs = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnrPerson));

        Assertions.assertEquals(1, barnFnrs.size());
        Assertions.assertEquals(barnFnrs.get(0), fnrBarn2);
    }

    private PDLPersonBarn pdlBarn() {
        return new PDLPersonBarn()
                .setErIlive(true)
                .setFodselsdato(LocalDate.of(2004, 12, 12))
                .setDiskresjonskode("6");
    }

    private PDLPersonBarn pdlBarn15Aar() {
        LocalDate iDag = LocalDate.now();
        LocalDate fodselsdato15Aar = iDag.minusYears(15).minusMonths(1);
        return new PDLPersonBarn()
                .setErIlive(true)
                .setFodselsdato(fodselsdato15Aar)
                .setDiskresjonskode("6");
    }

    private PDLPersonBarn pdlBarn18Aar() {
        LocalDate iDag = LocalDate.now();
        LocalDate fodselsdato15Aar = iDag.minusYears(18).minusMonths(1);
        return new PDLPersonBarn()
                .setErIlive(true)
                .setFodselsdato(fodselsdato15Aar)
                .setDiskresjonskode("");
    }

    private PDLPersonBarn pdlBarn4Aar() {
        LocalDate iDag = LocalDate.now();
        LocalDate fodselsdato15Aar = iDag.minusYears(4).minusMonths(5);
        return new PDLPersonBarn()
                .setErIlive(true)
                .setFodselsdato(fodselsdato15Aar)
                .setDiskresjonskode("");
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
