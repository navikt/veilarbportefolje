package no.nav.pto.veilarbportefolje.tiltaksaktivitet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.dto.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class TiltaksaktivitetService extends KafkaCommonNonKeyedConsumerService<KafkaAktivitetMelding> {
    private final TiltaksaktivitetRepository tiltaksaktivitetRepository;

    public void behandleKafkaMeldingLogikk(KafkaAktivitetMelding kafkaMelding) {
        if (!validerMelding(kafkaMelding)) {
            return;
        }

        AktorId aktorId = AktorId.of(kafkaMelding.getAktorId());
        String aktivitetId = kafkaMelding.getAktivitetId();

        if (kafkaMelding.isHistorisk()) {
            secureLog.info("Sletter tiltaksaktivitet fra tabellen tiltaksAktivitet med tiltakskode {}, pa aktoer: {}", aktivitetId, kafkaMelding.getTiltakskode(), aktorId);
            tiltaksaktivitetRepository.deleteTiltaksaktivitet(aktivitetId);
        } else if (erNyVersjonAvAktivitet(kafkaMelding)) {
            secureLog.info("Lagrer tiltaksaktivitet i tabellen tiltaksAktivitet med tiltakskode {}, pa aktoer: {}", aktivitetId, kafkaMelding.getTiltakskode(), aktorId);
            tiltaksaktivitetRepository.upsert(mapTilTiltakaktivitetEntity(kafkaMelding), aktorId);
        }
    }

    private boolean erNyVersjonAvAktivitet(KafkaAktivitetMelding aktivitet) {
        Long kommendeVersjon = aktivitet.getVersion();

        if (kommendeVersjon == null) {
            return false;
        }

        Long databaseVersjon = tiltaksaktivitetRepository.hentSistVersjonAvAktivitet(aktivitet.getAktivitetId());

        if (databaseVersjon == null) {
            return true;
        }

        return kommendeVersjon.compareTo(databaseVersjon) >= 0;
    }

    private boolean validerMelding(KafkaAktivitetMelding kafkaMelding) {
        if (kafkaMelding == null) {
            log.warn("Kafka-melding ignoreres i TiltaksaktivitetService fordi den er tom (null).");
            return false;
        }

        if (kafkaMelding.getAktivitetId() == null) {
            log.warn("Kafka-melding ignoreres i TiltaksaktivitetService: mangler aktivitetId.");
            return false;
        }

        if (kafkaMelding.getAktivitetType() != KafkaAktivitetMelding.AktivitetTypeData.TILTAK) {
            log.warn("Kafka-melding med aktivitetstype {} ignoreres i TiltaksaktivitetService, da den kun behandler aktiviteter med aktivitetstype TILTAK",
                    kafkaMelding.getAktivitetType());
            return false;
        }

        if(kafkaMelding.getTiltakskode() == null) {
            log.warn("Kafka-melding med aktivitetid {} ignoreres i TiltaksaktivitetService fordi det mangler tiltakskode", kafkaMelding.getAktivitetId());
            return false;
        }

        return true;
    }

    public static TiltakaktivitetEntity mapTilTiltakaktivitetEntity(KafkaAktivitetMelding kafkaMelding) {
        if (kafkaMelding == null) {
            return null;
        }

        return new TiltakaktivitetEntity()
                .setAktivitetId(kafkaMelding.getAktivitetId())
                .setFraDato(ArenaDato.of(kafkaMelding.getFraDato()))
                .setTilDato(ArenaDato.of(kafkaMelding.getTilDato()))
                .setTiltakskode(kafkaMelding.getTiltakskode())
                .setTiltaksnavn(Tiltakskodeverk.mapTiltakskodeTilNavn(kafkaMelding.getTiltakskode()))
                .setVersion(kafkaMelding.getVersion())
                .setStatus(Optional.ofNullable(kafkaMelding.getAktivitetStatus()).map(status -> status.name().toLowerCase()).orElse(null));
    }
}
