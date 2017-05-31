package no.nav.fo.domene.feed;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetDataFraFeed implements Comparable<AktivitetDataFraFeed> {

    String aktivitetId;
    String aktorId;

    Date fraDato;
    Date tilDato;
    Date opprettetDato;

    String aktivitetType;
    String status;
    boolean avtalt;

    @Override
    public int compareTo(AktivitetDataFraFeed o) {
        return opprettetDato.compareTo(o.opprettetDato);
    }
}