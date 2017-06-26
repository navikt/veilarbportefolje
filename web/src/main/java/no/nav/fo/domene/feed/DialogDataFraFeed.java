package no.nav.fo.domene.feed;

import java.util.Date;

public class DialogDataFraFeed implements Comparable<DialogDataFraFeed> {
    public String aktorId;
    public Date sisteEndring;
    public Date tidspunktEldsteVentende;
    public Date tidspunktEldsteUbehandlede;

    @Override
    public int compareTo(DialogDataFraFeed o) {
        return o.sisteEndring.compareTo(this.sisteEndring);
    }
}
