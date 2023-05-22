package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class BarnUnder18AarServiceTest {

    @MockBean
    private BarnUnder18AarRepository barnUnder18AarRepository;

    private BarnUnder18AarService barnUnder18AarService;

    @BeforeEach
    public void setUp() {
        barnUnder18AarService = new BarnUnder18AarService(barnUnder18AarRepository);
    }

    @Test
    public void testHentBarnUnder18Aar() {
        List<Fnr> foreldre = List.of(randomFnr(), randomFnr());
        List<Fnr> barn = List.of(randomFnr(), randomFnr(), randomFnr(), randomFnr(), randomFnr());

        List<BarnUnder18AarData> barnInfo = List.of(new BarnUnder18AarData(17, null),
                new BarnUnder18AarData(16, "7"),
                new BarnUnder18AarData(12, null),
                new BarnUnder18AarData(8, "6"),
                new BarnUnder18AarData(2, null));

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(0)))
                .thenReturn(barn.subList(0, 3));

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(1)))
                .thenReturn(barn.subList(3, 5));

        Mockito.when(barnUnder18AarRepository.hentInfoOmBarn(barn.get(0))).thenReturn(barnInfo.get(0));
        Mockito.when(barnUnder18AarRepository.hentInfoOmBarn(barn.get(1))).thenReturn(barnInfo.get(1));
        Mockito.when(barnUnder18AarRepository.hentInfoOmBarn(barn.get(2))).thenReturn(barnInfo.get(2));
        Mockito.when(barnUnder18AarRepository.hentInfoOmBarn(barn.get(3))).thenReturn(barnInfo.get(3));
        Mockito.when(barnUnder18AarRepository.hentInfoOmBarn(barn.get(4))).thenReturn(barnInfo.get(4));

        Map<Fnr, List<BarnUnder18AarData>> barnUnder18AarInfo = barnUnder18AarService.hentBarnUnder18Aar(foreldre);

        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(0)).size(), 3);
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(0)).get(0), barnInfo.get(0));
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(0)).get(1), barnInfo.get(1));
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(0)).get(2), barnInfo.get(2));
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(1)).size(), 2);
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(1)).get(0), barnInfo.get(3));
        Assertions.assertEquals(barnUnder18AarInfo.get(foreldre.get(1)).get(1), barnInfo.get(4));
    }

    @Test
    public void testHentBarnFnrsForForeldre() {
        List<Fnr> foreldre = List.of(randomFnr(), randomFnr());
        List<Fnr> barn = List.of(randomFnr(), randomFnr(), randomFnr(), randomFnr(), randomFnr());

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(0)))
                .thenReturn(barn.subList(0, 3));

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(1)))
                .thenReturn(barn.subList(3, 5));

        List<Fnr> fnrBarnRespons = barnUnder18AarService.hentBarnFnrsForForeldre(foreldre);
        Assertions.assertEquals(fnrBarnRespons.size(), 5);
        Assertions.assertEquals(fnrBarnRespons, barn);
    }

    @Test
    public void testHentBarnFnrsForForeldre_fellesBarn() {
        List<Fnr> foreldre = List.of(randomFnr(), randomFnr());
        List<Fnr> barn = List.of(randomFnr(), randomFnr(), randomFnr(), randomFnr(), randomFnr());

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(0)))
                .thenReturn(barn.subList(0, 3));

        Mockito.when(barnUnder18AarRepository.hentForeldreansvarForPerson(foreldre.get(1)))
                .thenReturn(barn.subList(2, 5));

        List<Fnr> fnrBarnRespons = barnUnder18AarService.hentBarnFnrsForForeldre(foreldre);
        Assertions.assertEquals(fnrBarnRespons.size(), 6);
        Assertions.assertTrue(fnrBarnRespons.containsAll(barn));
    }


}