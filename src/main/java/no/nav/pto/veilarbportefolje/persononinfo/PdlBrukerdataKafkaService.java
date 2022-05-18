package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlBrukerdataKafkaService extends KafkaCommonConsumerService<String> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchIndexer opensearchIndexer;

    @Override
    @SneakyThrows
    public void behandleKafkaMeldingLogikk(String melding) {
        PdlDokument pdlDokument = objectMapper.readValue(melding.substring(melding.indexOf("{")), PdlDokument.class);
        if (pdlDokument == null || pdlDokument.getHentPerson() == null || pdlDokument.getHentIdenter() == null) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        PDLPerson person = PDLPerson.genererFraApiRespons(pdlDokument.getHentPerson());
        List<AktorId> aktorIds = pdlDokument.getHentIdenter().getIdenter().stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();

        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)) {
            AktorId aktorId = pdlIdentRepository.hentAktorId(person.getFnr());

            log.info("Det oppsto en brukerdata endring aktoer: {}", aktorId);
            pdlPersonRepository.upsertPerson(person);
            opensearchIndexer.indekser(aktorId);
        }
    }
}
