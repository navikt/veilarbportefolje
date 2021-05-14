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
        public static final String ER_DOED = "ER_DOED";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
        public static final String VEDTAKSTATUS = "VEDTAKSTATUS";
        public static final String VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN = "VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN";
        public static final String VEDTAKSTATUS_ENDRET_TIDSPUNKT = "VEDTAKSTATUS_ENDRET_TIDSPUNKT";
    }


    public static final class OPTIMALISER_BRUKER_VIEW {
        public static final String TABLE_NAME = "OPTIMALISER_BRUKER";

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

        public static final String SQLINSERT_STRING = AKTOERID +
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

        public static final String SQLUPDATE_STRING = FODSELSNR +
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

        public static final String SQLINSERT_STRING =
                AKTOERID +
                        ", " + VENTER_PA_BRUKER +
                        ", " + VENTER_PA_NAV;

        public static final String SQLUPDATE_STRING =
                VENTER_PA_BRUKER +
                        ", " + VENTER_PA_NAV;

    }

    public static final class ARBEIDSLISTE {
        public static final String TABLE_NAME = "ARBEIDSLISTE";

        public static final String AKTOERID = "AKTOERID";
        public static final String SIST_ENDRET_AV_VEILEDERIDENT = "SIST_ENDRET_AV_VEILEDERIDENT";
        public static final String ENDRINGSTIDSPUNKT = "ENDRINGSTIDSPUNKT";
        public static final String OVERSKRIFT = "OVERSKRIFT";
        public static final String KOMMENTAR = "KOMMENTAR";
        public static final String FRIST = "FRIST";
        public static final String KATEGORI = "KATEGORI";
        public static final String NAV_KONTOR_FOR_ARBEIDSLISTE = "NAV_KONTOR_FOR_ARBEIDSLISTE";
        public static final String UPDATE_STRING = SIST_ENDRET_AV_VEILEDERIDENT +
                            ", " + ENDRINGSTIDSPUNKT +
                            ", " + OVERSKRIFT +
                            ", " + KOMMENTAR +
                            ", " + FRIST +
                            ", " + KATEGORI +
                            ", " + NAV_KONTOR_FOR_ARBEIDSLISTE;
    }

    public static final class VEDTAKSTATUS {
        public static final String TABLE_NAME = "VEDTAKSTATUS";

        public static final String AKTOERID = "AKTOERID";
        public static final String VEDTAKID = "VEDTAKID";
        public static final String VEDTAKSTATUS = "VEDTAKSTATUS";
        public static final String INNSATSGRUPPE = "INNSATSGRUPPE";
        public static final String HOVEDMAL = "HOVEDMAL";
        public static final String ANSVARLIG_VEILDERIDENT = "ANSVARLIG_VEILDERIDENT";
        public static final String ANSVARLIG_VEILDERNAVN = "ANSVARLIG_VEILDERNAVN";
        public static final String ENDRET_TIDSPUNKT = "ENDRET_TIDSPUNKT";

        public static final String SQLINSERT_STRING = AKTOERID +
                ", " + VEDTAKID +
                ", " + VEDTAKSTATUS +
                ", " + INNSATSGRUPPE +
                ", " + HOVEDMAL +
                ", " + ANSVARLIG_VEILDERIDENT +
                ", " + ANSVARLIG_VEILDERNAVN +
                ", " + ENDRET_TIDSPUNKT;

        public static final String SQLUPDATE_STRING =
                VEDTAKID +
                ", " + VEDTAKSTATUS +
                ", " + INNSATSGRUPPE +
                ", " + HOVEDMAL +
                ", " + ANSVARLIG_VEILDERIDENT +
                ", " + ANSVARLIG_VEILDERNAVN +
                ", " + ENDRET_TIDSPUNKT;
    }

    public static String safeNull(Object o) {
        if (o == null) {
            return "NULL";
        }
        return "'" + o.toString() + "'";
    }

    public static String safeBool(Object o) {
        if (o == null) {
            return "false";
        }
        return "'" + o.toString() + "'";
    }
}
