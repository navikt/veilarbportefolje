package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.person.pdl.aktor.v2.Aktor;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukBrukerDataTopicForIdenter;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivAktor;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.typeTilGruppe;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlIdentKafkaService extends KafkaCommonConsumerService<Aktor> {
    private final PdlIdentRepository pdlIdentRepository;
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMeldingLogikk(Aktor melding) {
        if (brukBrukerDataTopicForIdenter(unleashService)) {
            log.info("Ignorerer melding på ident topic, bruker nå aapen-person-pdl-dokument-v1 for ident håndtering");
            return;
        }
        if (melding == null || melding.getIdentifikatorer().size() == 0) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        List<PDLIdent> pdlIdenter = mapTilPdlIdenter(melding);
        List<AktorId> aktorIds = hentAktoerIder(pdlIdenter);
        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIds)) {
            AktorId aktoerId = hentAktivAktor(pdlIdenter);
            log.info("Det oppsto en id endring på en bruker under oppfølging. AktørId: {}", aktoerId);
            pdlIdentRepository.upsertIdenter(pdlIdenter);
        }
    }

    private List<AktorId> hentAktoerIder(List<PDLIdent> identer) {
        return identer.stream()
                .filter(x -> PDLIdent.Gruppe.AKTORID.equals(x.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new).toList();
    }

    private List<PDLIdent> mapTilPdlIdenter(Aktor melding) {
        return melding.getIdentifikatorer()
                .stream()
                .map(id -> new PDLIdent(id.getIdnummer().toString(), !id.getGjeldende(), typeTilGruppe(id.getType())))
                .toList();
    }
}
