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

    public static List<Bruker> sensurerBrukere(List<Bruker> brukere, String veilederIdent, PepClient pepClient) {
        return brukere.stream()
                .map( bruker -> bruker.erKonfidensiell() ? fjernKonfidensiellInfoDersomIkkeTilgang(bruker, veilederIdent, pepClient) : bruker)
                .collect(toList());
    }

    private static Bruker fjernKonfidensiellInfoDersomIkkeTilgang(Bruker bruker, String veilederIdent, PepClient pepClient) {
        String diskresjonskode = bruker.getDiskresjonskode() == null ? "" : bruker.getDiskresjonskode();
        Boolean egenAnsatt = bruker.getEgenAnsatt() == null ? false : bruker.getEgenAnsatt();
        if(diskresjonskode.equals("6") && !pepClient.isSubjectAuthorizedToSeeKode6(veilederIdent)) {
            return fjernKonfidensiellInfo(bruker);
        }
        if(diskresjonskode.equals("7") && !pepClient.isSubjectAuthorizedToSeeKode7(veilederIdent)) {
            return fjernKonfidensiellInfo(bruker);
        }
        if(egenAnsatt && !pepClient.isSubjectAuthorizedToSeeEgenAnsatt(veilederIdent)) {
            return fjernKonfidensiellInfo(bruker);
        }
        return bruker;

    }

    private static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setEtternavn("").setFornavn("").setKjonn("").setFodselsdato("");
    }
}
