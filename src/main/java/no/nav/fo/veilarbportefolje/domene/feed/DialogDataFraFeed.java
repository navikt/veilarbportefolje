package no.nav.fo.veilarbportefolje.domene.feed;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class DialogDataFraFeed implements Comparable<DialogDataFraFeed> {

    public static final String FEED_NAME = "dialogaktor";

    public String aktorId;
    public Date sisteEndring;
    public Date tidspunktEldsteVentende;
    public Date tidspunktEldsteUbehandlede;

    @Override
    public int compareTo(DialogDataFraFeed o) {
        return o.sisteEndring.compareTo(this.sisteEndring);
    }
}
