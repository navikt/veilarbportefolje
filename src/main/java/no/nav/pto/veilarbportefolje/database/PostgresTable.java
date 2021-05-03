package no.nav.pto.veilarbportefolje.database;

public class PostgresTable {

    public static final class BRUKER_VIEW {
        public static final String TABLE_NAME = "BRUKER";

        public static final String AKTOERID = "AKTOERID";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String STARTDATO = "STARTDATO";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
        public static final String VEILEDERID = "VEILEDERID";
        public static final String MANUELL = "MANUELL";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String FORNAVN = "FORNAVN";
        public static final String ETTERNAVN = "ETTERNAVN";
        public static final String NAV_KONTOR = "NAV_KONTOR";
        public static final String ISERV_FRA_DATO = "ISERV_FRA_DATO";
        public static final String FORMIDLINGSGRUPPEKODE = "FORMIDLINGSGRUPPEKODE";
        public static final String KVALIFISERINGSGRUPPEKODE = "KVALIFISERINGSGRUPPEKODE";
        public static final String RETTIGHETSGRUPPEKODE = "RETTIGHETSGRUPPEKODE";
        public static final String HOVEDMAALKODE = "HOVEDMAALKODE";
        public static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
        public static final String DISKRESJONSKODE = "DISKRESJONSKODE";
        public static final String HAR_OPPFOLGINGSSAK = "HAR_OPPFOLGINGSSAK";
        public static final String SPERRET_ANSATT = "SPERRET_ANSATT";
        public static final String DOED_FRA_DATO = "DOED_FRA_DATO";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
    }


    public static final class ESSENSIELL_BRUKER_VIEW {
        public static final String TABLE_NAME = "BRUKER";

        public static final String AKTOERID = "AKTOERID";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String STARTDATO = "STARTDATO";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
        public static final String VEILEDERID = "VEILEDERID";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String FORNAVN = "FORNAVN";
        public static final String ETTERNAVN = "ETTERNAVN";
        public static final String NAV_KONTOR = "NAV_KONTOR";
        public static final String DISKRESJONSKODE = "DISKRESJONSKODE";
    }

    public static final class OPPFOLGING_DATA {
        public static final String TABLE_NAME = "OPPFOLGING_DATA";

        public static final String AKTOERID = "AKTOERID";
        public static final String VEILEDERID = "VEILEDERID";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
        public static final String MANUELL = "MANUELL";
        public static final String STARTDATO = "STARTDATO";
    }

    public static final class OPPFOLGINGSBRUKER_ARENA {
        public static final String TABLE_NAME = "OPPFOLGINGSBRUKER_ARENA";

        public static final String AKTOERID = "AKTOERID";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String FORMIDLINGSGRUPPEKODE = "FORMIDLINGSGRUPPEKODE";
        public static final String ISERV_FRA_DATO = "ISERV_FRA_DATO";
        public static final String ETTERNAVN = "ETTERNAVN";
        public static final String FORNAVN = "FORNAVN";
        public static final String NAV_KONTOR = "NAV_KONTOR";
        public static final String KVALIFISERINGSGRUPPEKODE = "KVALIFISERINGSGRUPPEKODE";
        public static final String RETTIGHETSGRUPPEKODE = "RETTIGHETSGRUPPEKODE";
        public static final String HOVEDMAALKODE = "HOVEDMAALKODE";
        public static final String SIKKERHETSTILTAK_TYPE_KODE = "SIKKERHETSTILTAK_TYPE_KODE";
        public static final String DISKRESJONSKODE = "DISKRESJONSKODE";
        public static final String HAR_OPPFOLGINGSSAK = "HAR_OPPFOLGINGSSAK";
        public static final String SPERRET_ANSATT = "SPERRET_ANSATT";
        public static final String ER_DOED = "ER_DOED";
        public static final String DOED_FRA_DATO = "DOED_FRA_DATO";
        public static final String ENDRET_DATO = "ENDRET_DATO";

        public static final String SQLINSERT_STRING =
                AKTOERID +
                        ", " + FODSELSNR +
                        ", " + FORMIDLINGSGRUPPEKODE +
                        ", " + ISERV_FRA_DATO +
                        ", " + ETTERNAVN +
                        ", " + FORNAVN +
                        ", " + NAV_KONTOR +
                        ", " + KVALIFISERINGSGRUPPEKODE +
                        ", " + RETTIGHETSGRUPPEKODE +
                        ", " + HOVEDMAALKODE +
                        ", " + SIKKERHETSTILTAK_TYPE_KODE +
                        ", " + DISKRESJONSKODE +
                        ", " + HAR_OPPFOLGINGSSAK +
                        ", " + SPERRET_ANSATT +
                        ", " + ER_DOED +
                        ", " + DOED_FRA_DATO +
                        ", " + ENDRET_DATO;

        public static final String SQLUPDATE_STRING =
                FODSELSNR +
                        ", " + FORMIDLINGSGRUPPEKODE +
                        ", " + ISERV_FRA_DATO +
                        ", " + ETTERNAVN +
                        ", " + FORNAVN +
                        ", " + NAV_KONTOR +
                        ", " + KVALIFISERINGSGRUPPEKODE +
                        ", " + RETTIGHETSGRUPPEKODE +
                        ", " + HOVEDMAALKODE +
                        ", " + SIKKERHETSTILTAK_TYPE_KODE +
                        ", " + DISKRESJONSKODE +
                        ", " + HAR_OPPFOLGINGSSAK +
                        ", " + SPERRET_ANSATT +
                        ", " + ER_DOED +
                        ", " + DOED_FRA_DATO +
                        ", " + ENDRET_DATO;
    }

    public static final class DIALOG {
        public static final String TABLE_NAME = "DIALOG";

        public static final String AKTOERID = "AKTOERID";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
        public static final String SIST_OPPDATERT = "SIST_OPPDATERT";

        public static final String SQLINSERT_STRING =
                AKTOERID +
                        ", " + SIST_OPPDATERT +
                        ", " + VENTER_PA_BRUKER +
                        ", " + VENTER_PA_NAV;

        public static final String SQLUPDATE_STRING =
                SIST_OPPDATERT +
                        ", " + VENTER_PA_BRUKER +
                        ", " + VENTER_PA_NAV;

    }


    public static String safeNull(Object o) {
        if (o == null) {
            return "NULL";
        }
        return "'" + o.toString() + "'";
    }
}
