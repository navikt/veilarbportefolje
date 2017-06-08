package no.nav.fo.domene.feed;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class AktivitetDataFraFeed implements Comparable<AktivitetDataFraFeed> {

    String aktivitetId;
    String aktorId;

    Timestamp fraDato;
    Timestamp tilDato;
    Timestamp opprettetDato;

    String aktivitetType;
    String status;
    boolean avtalt;

    @Override
    public int compareTo(AktivitetDataFraFeed o) {
        return opprettetDato.compareTo(o.opprettetDato);
    }
}