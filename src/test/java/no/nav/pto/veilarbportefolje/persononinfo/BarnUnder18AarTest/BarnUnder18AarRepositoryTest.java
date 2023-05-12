package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18Aar;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.service.UnleashService;
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

    @BeforeEach
    public void setUp() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        final UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(anyString())).thenReturn(true);
        this.barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        this.barnUnder18AarService = new BarnUnder18AarService(new BarnUnder18AarRepository(db, db));
        this.pdlPersonRepository = new PdlPersonRepository(db, null);
    }

    private final static String AKTORID = randomAktorId().get();
    private final static String FNR = randomFnr().get();


    @Test
    public void sjekkAtBarnBlirRiktigInsertedITabellen() {
        Fnr fnrPerson = randomFnr();
        Fnr fnrBarn = randomFnr();
        LocalDate fDato = LocalDate.of(2010, 10, 10);
        //when(barnUnder18AarRepository.hentInfoOmBarn(fnrBarn)).thenReturn(new BarnUnder18AarData(15, "" ));
        pdlPersonRepository.upsertPerson(fnrPerson, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        List<BarnUnder18Aar> barnFraPdl = List.of(pdlBarn());
        barnUnder18AarService.lagreBarnOgForeldreansvar(fnrPerson, barnFraPdl);
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18AarAlle(List.of(fnrPerson));
        Assertions.assertFalse(forelderBarnMap.isEmpty());
        Assertions.assertTrue(forelderBarnMap.get(fnrPerson).get(0).getAlder().equals(15L));
    }

    @Test
    public void barnBlirInsertedPaToUlikeForeldreSkalKunneVæreEnITabellen() {
        Fnr foresatt1 = randomFnr();
        Fnr foresatt2 = randomFnr();
        Fnr fnrBarn = Fnr.of("12312312312");
        LocalDate fDato = LocalDate.of(2010, 10, 10);
        pdlPersonRepository.upsertPerson(foresatt1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlPersonRepository.upsertPerson(foresatt2, new PDLPerson().setKjonn(M).setFoedsel(LocalDate.now()));
        //when(barnUnder18AarRepository.hentInfoOmBarn(fnrBarn)).thenReturn(new BarnUnder18AarData(15, "" ));
        List<BarnUnder18Aar> barnFraPdl = List.of(pdlBarn());
        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt1, barnFraPdl);
        barnUnder18AarService.lagreBarnOgForeldreansvar(foresatt2, barnFraPdl);
        Map<Fnr, List<BarnUnder18AarData>> forelderBarnMap = barnUnder18AarService.hentBarnUnder18AarAlle(List.of(foresatt1));
        Assertions.assertTrue(forelderBarnMap.size()==1);
    }


    @Test
    public void girRiktigAlder_alderFraFodselsdato() {
        LocalDate referenceDate = LocalDate.of(2023, 5, 5);
        LocalDate fodselsdatoFylt12Aar = LocalDate.of(2010, 6, 5);
        LocalDate fodselsdatoFylt13Aar = LocalDate.of(2010, 4, 5);

        Long age12 = barnUnder18AarRepository.alderFraFodselsdato(fodselsdatoFylt12Aar, referenceDate);
        Long age13 = barnUnder18AarRepository.alderFraFodselsdato(fodselsdatoFylt13Aar, referenceDate);

        Assertions.assertTrue(age12.equals(12L));
        Assertions.assertTrue(age13.equals(13L));
    }

    private BarnUnder18Aar pdlBarn() {
        return new BarnUnder18Aar()
                .setFnr(Fnr.of("12312312312"))
                .setFodselsdato(LocalDate.of(2004, 12, 12))
                .setDiskresjonskode("6");
    }

    private BarnUnder18Aar pdlBarn15Aar() {
        return new BarnUnder18Aar()
                .setFnr(Fnr.of("12312312312"))
                .setFodselsdato(LocalDate.of(2004, 12, 12))
                .setDiskresjonskode("6");
    }

}
