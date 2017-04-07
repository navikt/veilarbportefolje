package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BrukerOppdatertInformasjon {

    private String aktoerid;
    private String veileder;
    private String oppdatert;

    public BrukerinformasjonFraKo applyTo(BrukerinformasjonFraKo brukerinformasjonFraKo) {
        return brukerinformasjonFraKo
                .setOppdatert(oppdatert)
                .setVeileder(veileder)
                .setAktoerid(aktoerid);
    }
}
