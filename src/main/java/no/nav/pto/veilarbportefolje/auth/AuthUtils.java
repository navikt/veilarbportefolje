package no.nav.pto.veilarbportefolje.auth;

import no.nav.pto.veilarbportefolje.domene.Bruker;

import javax.ws.rs.ForbiddenException;

import static java.lang.String.format;

public class AuthUtils {

    static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setEtternavn("").setFornavn("").setKjonn("").setFodselsdato(null);
    }
    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ForbiddenException(format("sjekk av %s feilet, %s", navn, data));
        }
    }
}
