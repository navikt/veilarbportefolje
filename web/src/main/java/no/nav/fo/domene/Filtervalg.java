package no.nav.fo.domene;

import javax.ws.rs.QueryParam;
import java.util.List;

public class Filtervalg {
    @QueryParam("nyeBrukere")
    public boolean nyeBrukere;

    @QueryParam("inaktiveBrukere")
    public boolean inaktiveBrukere;

    @QueryParam("ytelser")
    public List<YtelseMapping> ytelser;

    public boolean harAktiveFilter() {
        return nyeBrukere || inaktiveBrukere || harYtelsefilter();
    }

    public boolean harYtelsefilter() {
        return (ytelser != null && !ytelser.isEmpty());
    }
}
