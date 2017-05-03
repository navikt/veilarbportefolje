package no.nav.fo.provider.rest;

import no.nav.fo.domene.Filtervalg;
import no.nav.fo.exception.RestValideringsfeilException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Provider
public class ValideringsRegler implements ExceptionMapper<RestValideringsfeilException> {
    private static List<String> sortDirs = asList("ikke_satt", "ascending", "descending");
    private static List<String> sortFields = asList("ikke_satt", "etternavn", "fodselsnummer", "utlopsdato", "aapMaxtid");

    public static void sjekkEnhet(String enhet) {
        test("enhet", enhet, enhet.matches("\\d{4}"));
    }


    public static void sjekkVeilederIdent(String veilederIdent) {
        test("veilederident", veilederIdent, veilederIdent.matches("[A-Z]\\d{6}"));
    }

    public static void sjekkFiltervalg(Filtervalg filtervalg) {
        test("filtervalg", filtervalg, filtervalg::valider);
    }

    public static void sjekkSortering(String sortDirection, String sortField) {
        test("sortDirection", sortDirection, sortDirs.contains(sortDirection));
        test("sortField", sortField, sortFields.contains(sortField));
    }

    private static void test(String navn, Object data, Supplier<Boolean> matches) {
        test(navn, data, matches.get());
    }

    private static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new RestValideringsfeilException(format("sjekk av %s feilet, %s", navn, data));
        }
    }

    @Override
    public Response toResponse(RestValideringsfeilException e) {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
    }
}
