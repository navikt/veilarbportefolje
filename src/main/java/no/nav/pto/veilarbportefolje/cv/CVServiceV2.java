package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Cv;
import no.nav.arbeid.cv.avro.EndreCv;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
@Slf4j
public class CVServiceV2 extends KafkaCommonNonKeyedConsumerService<Melding> {
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final CVRepositoryV2 cvRepositoryV2;
    private final PdlIdentRepository pdlIdentRepository;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(String.valueOf(kafkaMelding.getAktoerId()));
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktoerId);
        boolean erMeldingstypeEndreEllerOpprett = erMeldingstypeEndreEllerOpprett(kafkaMelding);

        if (fnr != null) {
            if (erMeldingstypeEndreEllerOpprett) {
                Timestamp cvSistEndret = Optional.ofNullable(kafkaMelding.getEndreCv())
                        .map(EndreCv::getCv)
                        .map(Cv::getSistEndret)
                        .map(Timestamp::from)
                        .orElse(null);
                secureLog.info("Oppdater CV eksisterer i BRUKER_REGISTRERT_CV tabell for bruker med aktoerid: {}, eksisterer: {}", aktoerId.get(), true);
                cvRepositoryV2.upsertCvRegistrert(fnr, cvSistEndret, true);
            } else {
                secureLog.info("Slett CV eksisterer fra BRUKER_REGISTRERT_CV for bruker med aktoerid: {}", aktoerId.get());
                cvRepositoryV2.slettCvRegistrert(fnr);
            }
        } else {
            secureLog.error("Bruker med aktoerid {} er ikke aktiv. FNR ikke funnet", aktoerId);
        }

        opensearchIndexerPaDatafelt.updateCvEksistere(aktoerId, erMeldingstypeEndreEllerOpprett);
    }

    public void slettCvData(AktorId aktorId, Optional<Fnr> maybeFnr) {
        if (maybeFnr.isEmpty()) {
            secureLog.warn("Kunne ikke slette CV-data for bruker med Aktør-ID {}. Årsak: fødselsnummer-parameter var tom.", aktorId.get());
            return;
        }

        try {
            cvRepositoryV2.slettCvRegistrert(maybeFnr.get());
        } catch (Exception e) {
            secureLog.error("Feil ved sletting av CV-data for bruker med fnr: {}", maybeFnr.get(), e);
        }
    }

    private boolean erMeldingstypeEndreEllerOpprett(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }
}
