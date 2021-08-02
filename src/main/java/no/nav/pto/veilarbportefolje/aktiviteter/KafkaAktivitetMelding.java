package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class KafkaAktivitetMelding {
    String aktivitetId;
    String aktorId;
    ZonedDateTime fraDato;
    ZonedDateTime tilDato;
    ZonedDateTime endretDato;
    Long version;
    AktivitetTypeData aktivitetType;
    AktivitetStatus aktivitetStatus;
    EndringsType endringsType;
    InnsenderData lagtInnAv;
    boolean avtalt;
    boolean historisk;

    public enum AktivitetStatus {
        PLANLAGT,
        GJENNOMFORES,
        FULLFORT,
        BRUKER_ER_INTERESSERT,
        AVBRUTT;

    }

    public enum AktivitetTypeData {
        EGEN,
        STILLING,
        SOKEAVTALE,
        IJOBB,
        BEHANDLING,
        MOTE,
        SAMTALEREFERAT,
    }

    public enum EndringsType {
        OPPRETTET,
        FLYTTET,
        REDIGERT,
        HISTORISK
    }

    public enum InnsenderData {
        BRUKER,
        NAV
    }

}
