package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class OppfolgingBruker implements Comparable<OppfolgingBruker> {

    String aktoerid;
    String veileder;
    Boolean oppfolging;
    Timestamp endretTimestamp;

    public OppfolgingBruker(){
        this.endretTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public String toString() {
        return "{\"aktoerid\":\""+aktoerid+"\",\"veileder\":\""+veileder+"\",\"oppdatert\":\""+endretTimestamp.toString()+"\"}";
    }

    @Override
    public int compareTo(OppfolgingBruker o) {
        return endretTimestamp.compareTo(o.endretTimestamp);
    }
}