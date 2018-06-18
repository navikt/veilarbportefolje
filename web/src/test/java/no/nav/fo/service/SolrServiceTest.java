package no.nav.fo.service;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.config.RemoteFeatureConfig;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.domene.PersonId;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.fo.mock.AktoerServiceMock.AKTOER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SolrServiceTest {

    @Mock
    private BrukerRepository brukerRepository;
    @Mock
    private AktivitetDAO aktivitetDAO;
    @Mock
    private SolrClient solrClientSlave;
    @Mock
    private SolrClient solrClientMaster;
    @Mock
    private AktoerService aktoerService;
    @Mock
    private VeilederService veilederService;
    @Mock
    private RemoteFeatureConfig.FlyttSomNyeFeature flyttSomNyeFeature;

    private SolrServiceImpl service;

    @Before
    public void setup() {
        service = new SolrServiceImpl(solrClientMaster, solrClientSlave, brukerRepository, aktoerService, veilederService, aktivitetDAO, flyttSomNyeFeature);
    }

    @Test
    public void deltaindekseringSkalOppdatereTidsstempel() throws Exception {
        SolrInputDocument dummyDocument = new SolrInputDocument();
        dummyDocument.addField("person_id", "dummy");
        dummyDocument.addField("fnr", "11111111111");
        dummyDocument.addField("oppfolging", true);
        dummyDocument.addField("kvalifiseringsgruppekode", "dontcare");
        dummyDocument.addField("formidlingsgruppekode", "dontcare");
        Map<PersonId, Optional<AktoerId>> personIdToAktoerid = new HashMap<>();
        personIdToAktoerid.put(PersonId.of("dummy"), Optional.of(AktoerId.of(AKTOER_ID)));
        when(brukerRepository.retrieveOppdaterteBrukere()).thenReturn(singletonList(dummyDocument));
        when(aktoerService.hentAktoeridsForPersonids(any())).thenReturn(personIdToAktoerid);

        System.setProperty("cluster.ismasternode", "true");

        service.deltaindeksering();

        verify(brukerRepository, atLeastOnce()).updateIndeksertTidsstempel(any(Timestamp.class));
    }

    @Test
    public void deltaindekseringSkalIkkeOppdatereTidsstempel() throws Exception {
        when(brukerRepository.retrieveOppdaterteBrukere()).thenReturn(emptyList());
        System.setProperty("cluster.ismasternode", "true");

        service.deltaindeksering();

        verify(brukerRepository, never()).updateIndeksertTidsstempel(any(Timestamp.class));
    }

    @Test
    public void hentBrukereForEnhet() throws IOException, SolrServerException {
        ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
        when(solrClientSlave.query(any(SolrQuery.class))).thenReturn(queryResponse(0, new SolrDocumentList()));

        service.hentBrukere("0100", Optional.empty(), "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrClientSlave, times(1)).query(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("*:*");
        assertThat(captor.getValue().getFilterQueries().length).isEqualTo(1);
        assertThat(captor.getValue().getFilterQueries()[0]).isEqualTo("enhet_id: 0100");
    }

    @Test
    public void hentBrukereForVeileder() throws IOException, SolrServerException {
        ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
        when(solrClientSlave.query(any(SolrQuery.class))).thenReturn(queryResponse(0, new SolrDocumentList()));

        service.hentBrukere("0100", Optional.of("Z900000"), "ikke_satt", "ikke_satt", new Filtervalg());

        verify(solrClientSlave, times(1)).query(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("*:*");
        assertThat(captor.getValue().getFilterQueries().length).isEqualTo(1);
        assertThat(captor.getValue().getFilterQueries()[0]).isEqualTo("veileder_id: Z900000 AND enhet_id: 0100");
    }

    @Test
    public void byggQueryString() {
        assertThat(service.byggQueryString("0100", Optional.empty())).isEqualTo("enhet_id: 0100");
        assertThat(service.byggQueryString("0100", Optional.of(""))).isEqualTo("enhet_id: 0100");
        assertThat(service.byggQueryString("0100", Optional.of("Z900000"))).isEqualTo("veileder_id: Z900000 AND enhet_id: 0100");
    }

    @Test
    public void skalFjerneFraIndeksOmBrukerIkkeErUnderOppfolging() throws Exception {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.setField("oppfolging",false);
        solrInputDocument.setField("person_id","dummy");
        solrInputDocument.setField("kvalifiseringsgruppekode","dummy");
        solrInputDocument.setField("formidlingsgruppekode","dummy");
        when(brukerRepository.retrieveBrukermedBrukerdata(any())).thenReturn(solrInputDocument);
        when(solrClientMaster.deleteByQuery("person_id:dummy")).thenReturn(mock(UpdateResponse.class));

        service.indekserBrukerdata(PersonId.of("dummy"));
        verify(solrClientMaster, never()).add(any(Collection.class));
        verify(solrClientMaster).deleteByQuery("person_id:dummy");
    }

    private QueryResponse queryResponse(int status, SolrDocumentList data) {
        QueryResponse response = new QueryResponse();
        NamedList<Object> responseData = new NamedList<>();
        NamedList<Object> headerData = new NamedList<>();
        headerData.add("status", status);
        responseData.add("responseHeader", headerData);
        responseData.add("response", data);
        response.setResponse(responseData);

        return response;
    }
}
