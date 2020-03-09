package no.nav.pto.veilarbportefolje.kafka.aktivitet;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class KafkaAktivitetMelding {
    String meldingId;
    Long aktivitetId;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeData aktivitetType;
    AktivitetStatus aktivitetStatus;
    Boolean avtalt;
    Boolean historisk;

    enum AktivitetTypeData {
        EGENAKTIVITET,
        JOBBSOEKING,
        SOKEAVTALE,
        IJOBB,
        BEHANDLING,
        MOTE,
        SAMTALEREFERAT,
    }

    enum AktivitetStatus {
        PLANLAGT,
        GJENNOMFORES,
        FULLFORT,
        BRUKER_ER_INTERESSERT,
        AVBRUTT;

    }
}
