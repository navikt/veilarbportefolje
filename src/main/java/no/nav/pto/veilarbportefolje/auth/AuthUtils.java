package no.nav.pto.veilarbportefolje.auth;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static java.lang.String.format;

public class AuthUtils {

    static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setEtternavn("").setFornavn("").setKjonn("").setFodselsdato(null);
    }

    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, format("sjekk av %s feilet, %s", navn, data));
        }
    }

    public static String getInnloggetBrukerToken() {
        return AuthContextHolderThreadLocal
                .instance().getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"));
    }

    public static VeilederId getInnloggetVeilederIdent() {
        return AuthContextHolderThreadLocal
                .instance().getNavIdent()
                .map(id -> VeilederId.of(id.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is missing from subject"));
    }

    public static Optional<String> getIdTokenString() {
        return AuthContextHolderThreadLocal
                .instance().getIdTokenString();
    }
}
