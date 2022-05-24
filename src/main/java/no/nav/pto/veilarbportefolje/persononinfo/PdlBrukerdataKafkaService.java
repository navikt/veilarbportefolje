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
            AktorId aktorId = hentAktivAktoer(pdlDokument.getHentIdenter().getIdenter());
            log.info("Det oppsto en brukerdata endring aktoer: {}", aktorId);

            PDLPerson person = PDLPerson.genererFraApiRespons(pdlDokument.getHentPerson(), aktorId);
            pdlPersonRepository.upsertPerson(person);
            opensearchIndexer.indekser(aktorId);
        }
    }


    private AktorId hentAktivAktoer(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .filter(pdlIdent -> !pdlIdent.isHistorisk())
                .map(PDLIdent::getIdent)
                .map(AktorId::new)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Ingen aktiv ident på bruker"));
    }
}
