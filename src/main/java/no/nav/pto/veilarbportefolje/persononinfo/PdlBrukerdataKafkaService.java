package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukBrukerDataTopicForIdenter;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivAktor;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlBrukerdataKafkaService extends KafkaCommonConsumerService<String> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchIndexer opensearchIndexer;
    private final UnleashService unleashService;

    @Override
    @SneakyThrows
    public void behandleKafkaMeldingLogikk(String pdlDokumentJson) {
        if (pdlDokumentJson == null) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        PdlDokument pdlDokument = objectMapper.readValue(pdlDokumentJson, PdlDokument.class);
        List<AktorId> aktorIds = pdlDokument.getHentIdenter().getIdenter().stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();

        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)) {
            AktorId aktorId = hentAktivAktor(pdlDokument.getHentIdenter().getIdenter());
            Fnr fnr = hentAktivFnr(pdlDokument.getHentIdenter().getIdenter());
            log.info("Det oppsto en brukerdata endring aktoer: {}", aktorId);

            PDLPerson person = PDLPerson.genererFraApiRespons(pdlDokument.getHentPerson());
            if (brukBrukerDataTopicForIdenter(unleashService)) {
                pdlIdentRepository.upsertIdenter(pdlDokument.getHentIdenter().getIdenter());
            }
            pdlPersonRepository.upsertPerson(fnr, person);
            opensearchIndexer.indekser(aktorId);
        }
    }
}
