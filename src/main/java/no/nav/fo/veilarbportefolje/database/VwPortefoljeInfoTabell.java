package no.nav.fo.veilarbportefolje.database;

import no.nav.sbl.sql.where.WhereClause;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbportefolje.database.VwPortefoljeInfoTabell.Kolonne.*;
import static no.nav.sbl.sql.where.WhereClause.in;

public class VwPortefoljeInfoTabell {

    public static final String VW_PORTEFOLJE_INFO = "VW_PORTEFOLJE_INFO";

    public static class Kolonne {
        public static final String FODSELSNR = "FODSELSNR";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSON_ID = "PERSON_ID";
        public static final String NAV_KONTOR = "NAV_KONTOR";
        public static final String FORMIDLINGSGRUPPEKODE = "FORMIDLINGSGRUPPEKODE";
        public static final String KVALIFISERINGSGRUPPEKODE = "KVALIFISERINGSGRUPPEKODE";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String RESERVERTIKRR = "RESERVERTIKRR";
    }

    public static WhereClause brukerErUnderOppfolging() {
        return erArbeidssoker().or(harOppfolgingsFlaggSatt()).or(erIArbeidOgHarInnsatsbehov());
    }

    private static WhereClause erArbeidssoker() {
        return WhereClause.equals(Kolonne.FORMIDLINGSGRUPPEKODE, "ARBS");
    }

    private static WhereClause erIArbeidOgHarInnsatsbehov() {
        return WhereClause
                .equals(FORMIDLINGSGRUPPEKODE, "IARBS")
                .and(in(KVALIFISERINGSGRUPPEKODE, asList("BATT", "BFORM", "VARIG", "IKVAL", "VURDU", "OPPFI")));
    }

    private static WhereClause harOppfolgingsFlaggSatt() {
        return WhereClause.equals(OPPFOLGING, "J");
    }
}
