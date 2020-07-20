package no.nav.pto.veilarbportefolje.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;

@Data
@Accessors(chain = true)
public class FeedElement implements Comparable<FeedElement>{
    protected String id;
    protected BrukerOppdatertInformasjon element;

    public int compareTo(FeedElement other) {
        return element.compareTo(other.getElement());
    }
}
