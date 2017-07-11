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
    ManedFasettMapping utlopsdatoFasett;
    Integer dagputlopUke;
    DagpengerUkeFasettMapping dagputlopUkeFasett;
    Integer permutlopUke;
    DagpengerUkeFasettMapping permutlopUkeFasett;
    Integer aapmaxtidUke;
    AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett;

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
                .setUtlopsFasett(utlopsdatoFasett)
                .setDagputlopUke(dagputlopUke)
                .setDagputlopUkeFasett(dagputlopUkeFasett)
                .setPermutlopUke(permutlopUke)
                .setPermutlopUkeFasett(permutlopUkeFasett)
                .setAapmaxtidUke(aapmaxtidUke)
                .setAapmaxtidUkeFasett(aapmaxtidUkeFasett);
    }
}
