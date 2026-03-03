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
import java.time.Instant;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
@Slf4j
public class CVService1 extends KafkaCommonNonKeyedConsumerService<Melding> {
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final CVRepositoryV2 cvRepositoryV2;
    private final PdlIdentRepository pdlIdentRepository;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(String.valueOf(kafkaMelding.getAktoerId()));
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktoerId);
        boolean cvEksisterer = cvEksistere(kafkaMelding);

        if (fnr != null) {
            if (cvEksisterer) {
                Optional<Instant> sistEndret = Optional.ofNullable(kafkaMelding.getEndreCv()).map(EndreCv::getCv).map(Cv::getSistEndret);
                Timestamp cvSistEndret = sistEndret.map(Timestamp::from).orElse(null);
                secureLog.info("Oppdater CV eksisterer i BRUKER_REGISTRERT_CV tabell for bruker med aktoerid: {}, eksisterer: {}", aktoerId.get(), true);
                cvRepositoryV2.upsertCVEksistererINyTabell(fnr, cvSistEndret, true);
            } else {
                secureLog.info("Slett CV eksisterer fra BRUKER_REGISTRERT_CV for bruker med aktoerid: {}", aktoerId.get());
                cvRepositoryV2.slettCvEksistererFraNyTabell(fnr);
            }
        }else {
            secureLog.error("Bruker med aktoerid {} er ikke aktiv. FNR ikke funnet", aktoerId);
        }

        opensearchIndexerPaDatafelt.updateCvEksistere(aktoerId, cvEksisterer);
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }
}
