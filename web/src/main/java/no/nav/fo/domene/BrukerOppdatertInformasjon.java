package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class BrukerOppdatertInformasjon implements Comparable<BrukerOppdatertInformasjon> {

    public static final String FEED_NAME = "oppfolging";

    private String aktoerid;
    private String veileder;
    private Boolean oppfolging;
    private Boolean nyForVeileder;
    private Timestamp endretTimestamp;


    public BrukerinformasjonFraFeed applyTo(BrukerinformasjonFraFeed brukerinformasjonFraFeed) {
        return brukerinformasjonFraFeed
                .setAktoerid(aktoerid)
                .setVeileder(veileder)
                .setOppfolging(oppfolging)
                .setOppdatert(endretTimestamp)
                .setNyForVeileder(nyForVeileder);
    }

    @Override
    public int compareTo(BrukerOppdatertInformasjon o) {
        return endretTimestamp.compareTo(o.getEndretTimestamp());
    }
}
