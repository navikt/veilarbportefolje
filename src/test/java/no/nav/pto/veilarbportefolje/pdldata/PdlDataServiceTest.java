package no.nav.pto.veilarbportefolje.pdldata;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PdlDataServiceTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private PdlRepository pdlRepository;

    @Mock
    private PdlClient pdlClient;

    @Mock
    private UnleashService unleashService;

    @Mock
    private AktorClient aktorClient;

    @InjectMocks
    private PdlDataService pdlDataService;

    @Before
    public void resetMock() {
        reset(brukerRepository, pdlRepository, pdlClient, unleashService, aktorClient);
    }

    private static final AktorId AKTOERID_TEST = AktorId.of("000000000");

    @Test
    @SneakyThrows
    public void parseDataFromPdl(){
        String mockreponsHentFodselsdag = getPdlMockRespons();

        when(unleashService.isEnabled(FeatureToggle.PDL)).thenReturn(true);
        when(pdlClient.rawRequest(Mockito.anyString())).thenReturn(mockreponsHentFodselsdag);
        pdlDataService.lastInnPdlData(AKTOERID_TEST);

        verify(pdlRepository).upsert(AKTOERID_TEST, DateUtils.getLocalDateFromSimpleISODate("1980-12-03"));
    }

    @Test
    @SneakyThrows
    public void parseDataFromVeilarbPerson(){

        when(unleashService.isEnabled(FeatureToggle.PDL)).thenReturn(false);
        pdlDataService.lastInnPdlData(AKTOERID_TEST);

        verify(pdlRepository).upsert(AKTOERID_TEST, DateUtils.getLocalDateFromSimpleISODate("1980-12-03"));
    }

    @SneakyThrows
    private String getPdlMockRespons() {
        val URI = getClass().getResource("/graphql/mockreponsHentFodselsdag.json").toURI();
        val encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }

}
