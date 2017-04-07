package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class BrukerinformasjonFraFil implements BrukerOppdatering {
    String personid;
    YtelseMapping ytelse;
    LocalDateTime utlopsdato;
    ManedMapping utlopsdatoFasett;
    LocalDateTime aapMaxtid;
    KvartalMapping aapMaxtidFasett;

    public BrukerinformasjonFraFil(String personid) {
        this.personid = personid;
    }

    @Override
    public String getPersonid() {
        return personid;
    }

    @Override
    public Brukerdata applyTo(Brukerdata bruker) {
        return bruker
                .setYtelse(ytelse)
                .setUtlopsdato(utlopsdato)
                .setUtlopsdatoFasett(utlopsdatoFasett)
                .setAapMaxtid(aapMaxtid)
                .setAapMaxtidFasett(aapMaxtidFasett);
    }
}
