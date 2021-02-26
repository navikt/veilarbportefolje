package no.nav.pto.veilarbportefolje.pdldata;

import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlError;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PdlDataServiceTest {

    @Mock
    private PdlRepository pdlRepository;

    @Mock
    private PdlClient pdlClient;

    @InjectMocks
    private PdlDataService pdlDataService;

    @Before
    public void resetMock() {
        reset(pdlRepository, pdlClient);
    }

    private static final AktorId AKTOERID_TEST = AktorId.of("000000000");

    @Test
    @SneakyThrows
    public void parseDataFromPdl(){
        PdlFodselsRespons mockRespons = new PdlFodselsRespons();
        mockRespons.setData(
                new PdlFodselsRespons.HentFodselsResponseData().setHentPerson(
                        new PdlFodselsRespons.HentFodselsResponseData.HentPersonDataResponsData().setFoedsel(
                                List.of(
                                        new PdlFodselsRespons.HentFodselsResponseData.HentPersonDataResponsData.Foedsel().setFoedselsdato("1980-12-03")
                                )
                        )
                )
        );
        mockRespons.setErrors(emptyList());
        when(pdlClient.request(Mockito.any(GraphqlRequest.class),Mockito.any())).thenReturn(mockRespons);
        pdlDataService.lastInnPdlData(AKTOERID_TEST);

        verify(pdlRepository).upsert(AKTOERID_TEST, DateUtils.getLocalDateFromSimpleISODate("1980-12-03"));
    }

    @Test
    public void graphql_respons_has_errors(){
        PdlFodselsRespons mockRespons = new PdlFodselsRespons();
        mockRespons.setErrors(List.of(new GraphqlError()));

        assertThat(PdlDataService.hasErrors(mockRespons)).isTrue();
    }

    @Test
    public void graphql_respons_has_no_Errors(){
        PdlFodselsRespons mockRespons = new PdlFodselsRespons();
        mockRespons.setErrors(emptyList());

        assertThat(PdlDataService.hasErrors(mockRespons)).isFalse();
    }

    @Test
    public void graphql_respons_is_null(){
        PdlFodselsRespons mockRespons = new PdlFodselsRespons();
        mockRespons.setErrors(null);

        assertThat(PdlDataService.hasErrors(mockRespons)).isTrue();
        assertThat(PdlDataService.hasErrors(null)).isTrue();
    }
}
