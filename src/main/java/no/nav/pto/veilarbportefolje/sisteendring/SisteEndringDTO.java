package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.mal.MalEndringKafkaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class SisteEndringDTO {
    private static final Logger log = LoggerFactory.getLogger(SisteEndringDTO.class);

    private AktorId aktoerId;
    private String aktivtetId;
    private SisteEndringsKategori kategori;
    private ZonedDateTime tidspunkt;

    public SisteEndringDTO() {
    }

    public SisteEndringDTO(MalEndringKafkaDTO melding) {
        aktoerId = AktorId.of(melding.getAktorId());
        tidspunkt = melding.getEndretTidspunk();
        kategori = SisteEndringsKategori.MAL;
        aktivtetId = null;
    }

    public SisteEndringDTO(KafkaAktivitetMelding melding) {
        aktoerId = AktorId.of(melding.getAktorId());
        tidspunkt = melding.getEndretDato();
        aktivtetId = melding.getAktivitetId();

        kategori = getSisteEndringsKategori(melding.getEndringsType(), melding.getAktivitetType(), melding.getAktivitetStatus());
    }

    public static SisteEndringsKategori getSisteEndringsKategori(KafkaAktivitetMelding.EndringsType endringsType,
                                                                 KafkaAktivitetMelding.AktivitetTypeData type,
                                                                 KafkaAktivitetMelding.AktivitetStatus status) {
        if (endringsType == null || type == null || status == null) {
            log.error("Et eller flere felt i aktivtet er null endringstype: {}, aktivitetstype: {}, aktivitetsstatus: {}",
                    endringsType,
                    type,
                    status
            );
            return null;
        }

        String sisteEndringsKategori = null;
        if (endringsType.equals(KafkaAktivitetMelding.EndringsType.OPPRETTET)) {
            sisteEndringsKategori = "NY_" + type.name();
        } else if (endringsType.equals(KafkaAktivitetMelding.EndringsType.FLYTTET)) {
            sisteEndringsKategori = status.name() + "_" + type.name();
        }

        if (SisteEndringsKategori.contains(sisteEndringsKategori)) {
            return SisteEndringsKategori.valueOf(sisteEndringsKategori);
        }
        return null;
    }
}