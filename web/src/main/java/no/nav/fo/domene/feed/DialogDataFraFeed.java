package no.nav.fo.domene.feed;

import java.util.Date;

public class DialogDataFraFeed implements Comparable<DialogDataFraFeed> {
    public String aktorId;
    public Date sisteEndring;
    public boolean venterPaSvar;
    public boolean harUbehandlet;

    @Override
    public int compareTo(DialogDataFraFeed o) {
        return o.sisteEndring.compareTo(this.sisteEndring);
    }
}
