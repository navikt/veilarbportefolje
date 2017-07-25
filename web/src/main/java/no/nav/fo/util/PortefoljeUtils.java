package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import no.nav.fo.service.PepClient;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class PortefoljeUtils {

    public static Portefolje buildPortefolje(List<Bruker> brukere, List<Bruker> brukereSublist, String enhet, int fra) {

        return new Portefolje()
                .setEnhet(enhet)
                .setBrukere(brukereSublist)
                .setAntallTotalt(brukere.size())
                .setAntallReturnert(brukereSublist.size())
                .setFraIndex(fra);
    }

    public static List<Bruker> getSublist(List<Bruker> brukere, int fra, int antall) {
        return brukere.stream().skip(fra).limit(antall).collect(toList());
    }

    public static List<Bruker> sensurerBrukere(List<Bruker> brukere, String token, PepClient pepClient) {
        return brukere.stream()
                .map( bruker -> fjernKonfidensiellInfoDersomIkkeTilgang(bruker, token, pepClient))
                .collect(toList());
    }

    private static Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker, String token, PepClient pepClient) {
        if(!bruker.erKonfidensiell()) {
            return bruker;
        }

        String diskresjonskode = bruker.getDiskresjonskode();

        if("6".equals(diskresjonskode) && !pepClient.isSubjectAuthorizedToSeeKode6(token)) {
            return fjernKonfidensiellInfo(bruker);
        }
        if("7".equals(diskresjonskode) && !pepClient.isSubjectAuthorizedToSeeKode7(token)) {
            return fjernKonfidensiellInfo(bruker);
        }
        if(bruker.isEgenAnsatt() && !pepClient.isSubjectAuthorizedToSeeEgenAnsatt(token)) {
            return fjernKonfidensiellInfo(bruker);
        }
        return bruker;

    }

    private static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setEtternavn("").setFornavn("").setKjonn("").setFodselsdato(null);
    }
}
