package no.nav.pto.veilarbportefolje.cv;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Cv;
import no.nav.arbeid.cv.avro.EndreCv;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import org.springframework.stereotype.Service;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
@Slf4j
public class CVService extends KafkaCommonNonKeyedConsumerService<Melding> {
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final CVRepositoryV2 cvRepositoryV2;
    private final PdlIdentRepository pdlIdentRepository;
    private final DefaultUnleash defaultUnleash;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(String.valueOf(kafkaMelding.getAktoerId()));
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktoerId);
        boolean cvEksisterer = cvEksistere(kafkaMelding);

        if (FeatureToggle.brukNyCvTabell(defaultUnleash)) {
            if (cvEksisterer) {
                Optional<Instant> sistEndret = Optional.ofNullable(kafkaMelding.getEndreCv()).map(EndreCv::getCv).map(Cv::getSistEndret);
                Timestamp cvSistEndret = sistEndret.map(Timestamp::from).orElse(null);
                secureLog.info("Oppdater CV eksisterer i BRUKER_REGISTRERT_CV tabell for bruker med aktoerid: {}, eksisterer: {}", aktoerId.get(), true);
                cvRepositoryV2.upsertCVEksistererINyTabell(fnr, cvSistEndret, true);
            } else {
                secureLog.info("Slett CV eksisterer fra BRUKER_REGISTRERT_CV for bruker med aktoerid: {}", aktoerId.get());
                cvRepositoryV2.slettCvEksistererFraNyTabell(fnr);
            }
        }
        else {
            if (cvEksisterer) {
                secureLog.info("Oppdater CV eksisterer for bruker med aktoerid: {}, eksisterer: {}", aktoerId.get(), true);
                cvRepositoryV2.upsertCVEksisterer(aktoerId, true);
            } else {
                secureLog.info("Slett CV eksisterer for bruker med aktoerid: {}", aktoerId.get());
                cvRepositoryV2.slettCvEksisterer(aktoerId);
            }
        }
            opensearchIndexerPaDatafelt.updateCvEksistere(aktoerId, cvEksisterer);
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }
}
