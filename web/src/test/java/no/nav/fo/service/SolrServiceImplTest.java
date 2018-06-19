package no.nav.fo.service;


import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.VeilederId;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static no.nav.fo.service.SolrServiceImpl.applyArbeidslisteData;
import static no.nav.fo.util.DateUtils.toZonedDateTime;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrServiceImplTest {

    private ArbeidslisteRepository arbeidslisteRepository = mock(ArbeidslisteRepository.class);
    private AktoerService aktoerService = mock(AktoerService.class);

    @Test
    public void skalLeggeTilArbeidslistedata() {
        PersonId personId = PersonId.of("111111");
        AktoerId aktoerId = AktoerId.of("222222");

        SolrInputDocument dokument = new SolrInputDocument();
        dokument.setField("person_id", personId.toString());
        Arbeidsliste arbeidsliste = new Arbeidsliste(VeilederId.of("X111111"), toZonedDateTime(new Date(0)), "overskrift", "kommentar", toZonedDateTime(new Date(0)));

        when(aktoerService.hentAktoeridsForPersonids(any())).thenReturn(singletonMap(personId, Optional.of(aktoerId)));
        when(arbeidslisteRepository.retrieveArbeidsliste(anyList())).thenReturn(singletonMap(aktoerId, Optional.of(arbeidsliste)));

        applyArbeidslisteData(singletonList(dokument),arbeidslisteRepository, aktoerService);
        assertThat(dokument.containsKey("arbeidsliste_aktiv")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_sist_endret_av_veilederid")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_endringstidspunkt")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_overskrift")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_kommentar")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_frist")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_er_oppfolgende_veileder")).isTrue();
    }

    @Test
    public void skalIkkeFeileOmFristIkkeErDefinert() {
        PersonId personId = PersonId.of("111111");
        AktoerId aktoerId = AktoerId.of("222222");

        SolrInputDocument dokument = new SolrInputDocument();
        dokument.setField("person_id", personId.toString());
        Arbeidsliste arbeidsliste = new Arbeidsliste(VeilederId.of("X111111"), toZonedDateTime(new Date(0)), "overskrift", "kommentar",null);

        when(aktoerService.hentAktoeridsForPersonids(any())).thenReturn(singletonMap(personId, Optional.of(aktoerId)));
        when(arbeidslisteRepository.retrieveArbeidsliste(anyList())).thenReturn(singletonMap(aktoerId, Optional.of(arbeidsliste)));

        applyArbeidslisteData(singletonList(dokument),arbeidslisteRepository, aktoerService);
        assertThat(dokument.containsKey("arbeidsliste_aktiv")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_sist_endret_av_veilederid")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_endringstidspunkt")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_overskrift")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_kommentar")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_frist")).isTrue();
        assertThat(dokument.containsKey("arbeidsliste_er_oppfolgende_veileder")).isTrue();
    }
}