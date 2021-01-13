package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class SisteEndringDTO {
    private AktoerId aktoerId;
    private SisteEndringsKategori kategori;
    private ZonedDateTime tidspunkt;

    public SisteEndringDTO() {
    }

    public SisteEndringDTO(KafkaAktivitetMelding melding) {
        aktoerId = AktoerId.of(melding.getAktorId());
        tidspunkt = melding.getEndretDato();

        if (melding.getEndringsType() != null || melding.getAktivitetType() == null || melding.getAktivitetStatus() == null) {
            String sisteEndringsKategori = null;
            if (melding.getEndringsType().equals(KafkaAktivitetMelding.EndringsType.OPPRETTET)) {
                sisteEndringsKategori = "NY_" + melding.getAktivitetType().name();
            } else if (melding.getEndringsType().equals(KafkaAktivitetMelding.EndringsType.FLYTTET)) {
                sisteEndringsKategori = melding.getAktivitetStatus().name() + "_" + melding.getAktivitetType().name();
            }

            if (SisteEndringsKategori.contains(sisteEndringsKategori)) {
                kategori = SisteEndringsKategori.valueOf(sisteEndringsKategori);
            }
        }
    }
}