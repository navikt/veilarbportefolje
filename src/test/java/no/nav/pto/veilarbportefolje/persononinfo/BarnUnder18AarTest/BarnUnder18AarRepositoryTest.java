package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
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
        this.barnUnder18AarRepository = new BarnUnder18AarRepository(null, db);
        this.pdlPersonRepository = new PdlPersonRepository(db, null);
    }

    private final static String AKTORID = randomAktorId().get();
    private final static String FNR = randomFnr().get();


    @Test
    public void sjekkAtBarnBlirRiktigInsertedITabellen() {
        Fnr fnr1 = randomFnr();
        Integer fnrBarn = 123;
        LocalDate fDato = LocalDate.of(2010, 10, 10);
        pdlPersonRepository.upsertPerson(fnr1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        //barnUnder18AarRepository.upsert(fnrBarn, fnr1, true, fDato, "");
        Map<Fnr, List<BarnUnder18AarData>> barn = barnUnder18AarService.hentBarnUnder18AarAlle(List.of(fnr1));
        Assertions.assertTrue(!barn.isEmpty());
        Assertions.assertTrue(barn.get(fnr1).get(0).getAlder().equals(12L));
    }

    @Test
    public void barnBlirInsertedPaToUlikeForeldre() {
        Fnr foresatt1 = randomFnr();
        Fnr foresatt2 = randomFnr();
        Integer fnrBarn = 1234;
        LocalDate fDato = LocalDate.of(2010, 10, 10);
        pdlPersonRepository.upsertPerson(foresatt1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlPersonRepository.upsertPerson(foresatt2, new PDLPerson().setKjonn(M).setFoedsel(LocalDate.now()));
        //barnUnder18AarRepository.upsert2(fnrBarn, foresatt1, true, fDato, "");
        //barnUnder18AarRepository.upsert2(fnrBarn, foresatt2, false, fDato, "");
        Map<Fnr, List<BarnUnder18AarData>> barn = barnUnder18AarService.hentBarnUnder18AarAlle(List.of(foresatt1));
        Assertions.assertTrue(barn.size()==2);
        Assertions.assertTrue(barn.get(foresatt1).get(0).getAlder().equals(12L));
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

}
