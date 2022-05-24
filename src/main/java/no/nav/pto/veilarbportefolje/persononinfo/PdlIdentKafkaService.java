package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.person.pdl.aktor.v2.Aktor;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.typeTilGruppe;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlIdentKafkaService extends KafkaCommonConsumerService<Aktor> {
    private final PdlIdentRepository pdlIdentRepository;

    @Override
    public void behandleKafkaMeldingLogikk(Aktor melding) {
        if (melding == null || melding.getIdentifikatorer().size() == 0) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        List<PDLIdent> pdlIdenter = mapTilPdlIdenter(melding);
        List<AktorId> aktorIds = pdlIdenter.stream()
                .filter(x -> PDLIdent.Gruppe.AKTORID.equals(x.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();
        if(pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)){
            log.info("Det oppsto en id endring på en bruker under oppfølging...");
            pdlIdentRepository.upsertIdenter(pdlIdenter);
        }
    }

    private List<PDLIdent> mapTilPdlIdenter(Aktor melding) {
        return melding.getIdentifikatorer()
                .stream()
                .map(id -> new PDLIdent(id.getIdnummer().toString(), !id.getGjeldende(), typeTilGruppe(id.getType())))
                .toList();
    }
}
