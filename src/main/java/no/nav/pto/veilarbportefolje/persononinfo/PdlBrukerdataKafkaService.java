package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
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
public class PdlBrukerdataKafkaService extends KafkaCommonConsumerService<PdlDokument> {
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchIndexer opensearchIndexer;

    @Override
    public void behandleKafkaMeldingLogikk(PdlDokument melding) {
        if (melding == null || melding.getHentPerson() == null || melding.getHentIdenter() == null) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        PDLPerson person = PDLPerson.genererFraApiRespons(melding.getHentPerson());
        List<AktorId> aktorIds = melding.getHentIdenter().getIdenter().stream()
                .filter(x -> PDLIdent.Gruppe.AKTORID.equals(x.getGruppe()))
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
