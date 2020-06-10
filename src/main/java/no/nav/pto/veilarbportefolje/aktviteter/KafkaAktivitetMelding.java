package no.nav.pto.veilarbportefolje.aktviteter;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class KafkaAktivitetMelding {
    String aktivitetId;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeData aktivitetType;
    AktivitetStatus aktivitetStatus;
    boolean avtalt;
    boolean historisk;
}