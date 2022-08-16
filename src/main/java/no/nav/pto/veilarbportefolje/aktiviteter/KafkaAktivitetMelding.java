package no.nav.pto.veilarbportefolje.aktiviteter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    CvKanDelesStatus cvKanDelesStatus;
    ZonedDateTime svarFristCvKanDeles;
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
        UTDANNINGAKTIVITET,
        STILLING_FRA_NAV
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

    public enum CvKanDelesStatus {
        JA,
        NEI,
        IKKE_SVART
    }

}
