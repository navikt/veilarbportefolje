package no.nav.fo.domene.feed;

import java.util.Date;

public class DialogDataFraFeed implements Comparable<DialogDataFraFeed> {
    public String aktorId;
    public Date sisteEndring;
    public Date venterPaSvar;
    public Date harUbehandlet;

    @Override
    public int compareTo(DialogDataFraFeed o) {
        return o.sisteEndring.compareTo(this.sisteEndring);
    }
}
