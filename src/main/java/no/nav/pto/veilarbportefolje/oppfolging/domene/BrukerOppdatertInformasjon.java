package no.nav.pto.veilarbportefolje.oppfolging.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class BrukerOppdatertInformasjon implements Comparable<BrukerOppdatertInformasjon> {
    private String aktoerid;
    private String veileder;
    private Boolean oppfolging;
    private Boolean nyForVeileder;
    private Timestamp endretTimestamp;
    private Timestamp startDato;
    private Boolean manuell;
    private BigDecimal feedId;
    private Timestamp tildeltTidspunkt;

    @Override
    public int compareTo(BrukerOppdatertInformasjon o) {
        return endretTimestamp.compareTo(o.getEndretTimestamp());
    }
}
