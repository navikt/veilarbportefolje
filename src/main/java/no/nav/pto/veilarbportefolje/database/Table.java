package no.nav.pto.veilarbportefolje.database;

public class Table {
    public static final String METADATA = "METADATA";

    public class Kolonner {
        static final String SIST_INDEKSERT_ES = "SIST_INDEKSERT_ES";
    }

    public static final class BRUKER_REGISTRERING {
        public static final String TABLE_NAME = "BRUKER_REGISTRERING";
        public static final String AKTOERID = "AKTOERID";
    }

    public static final class AKTOERID_TO_PERSONID {
        public static final String TABLE_NAME = "AKTOERID_TO_PERSONID";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSONID = "PERSONID";
        public static final String GJELDENE = "GJELDENE";
    }

    public static final class OPPFOLGING_DATA {
        public static final String TABLE_NAME = "OPPFOLGING_DATA";
        public static final String AKTOERID = "AKTOERID";
        public static final String VEILEDERIDENT = "VEILEDERIDENT";
        public static final String STARTDATO = "STARTDATO";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String MANUELL = "MANUELL";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
    }

    public static final class OPPFOLGINGSBRUKER {
        public static final String TABLE_NAME = "OPPFOLGINGSBRUKER";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String PERSON_ID = "PERSON_ID";
        public static final String NAV_KONTOR = "NAV_KONTOR";
    }

    public static final class ARBEIDSLISTE {
        public static final String TABLE_NAME = "ARBEIDSLISTE";
        public static final String FNR = "FNR";
        public static final String AKTOERID = "AKTOERID";
        public static final String SIST_ENDRET_AV_VEILEDERIDENT = "SIST_ENDRET_AV_VEILEDERIDENT";
        public static final String ENDRINGSTIDSPUNKT = "ENDRINGSTIDSPUNKT";
        public static final String OVERSKRIFT = "OVERSKRIFT";
        public static final String KOMMENTAR = "KOMMENTAR";
        public static final String FRIST = "FRIST";
        public static final String KATEGORI = "KATEGORI";
        public static final String NAV_KONTOR_FOR_ARBEIDSLISTE = "NAV_KONTOR_FOR_ARBEIDSLISTE";
    }

    public static final class VW_PORTEFOLJE_INFO {
        public static final String TABLE_NAME = "VW_PORTEFOLJE_INFO";
        public static final String AKTOERID = "AKTOERID";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String PERSON_ID = "PERSON_ID";
        public static final String NAV_KONTOR = "NAV_KONTOR";
    }

    public static final class BRUKER_DATA {
        public static final String TABLE_NAME = "BRUKER_DATA";
        public static final String PERSONID = "PERSONID";
        public static final String AKTOERID = "AKTOERID";
    }

    public static final class BRUKER_CV {
        public static final String TABLE_NAME = "BRUKER_CV";
        public static final String AKTOERID = "AKTOERID";
        public static final String FNR = "FNR";
        public static final String HAR_DELT_CV = "HAR_DELT_CV";
        public static final String SISTE_MELDING_MOTTATT = "SISTE_MELDING_MOTTATT";
    }

    public static final class SISTE_ENDRING {
        public static final String TABLE_NAME = "SISTE_ENDRING";
        public static final String AKTOERID = "AKTOERID";
        public static final String AKTIVITETID = "AKTIVITETID";

        public static final String SISTE_ENDRING_KATEGORI = "SISTE_ENDRING_KATEGORI";
        public static final String SISTE_ENDRING_TIDSPUNKT = "SISTE_ENDRING_TIDSPUNKT";
        public static final String ER_SETT = "ER_SETT";
    }

    public static final class AKTIVITETER {
        public static final String TABLE_NAME = "AKTIVITETER";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String AKTIVITETTYPE = "AKTIVITETTYPE";
        public static final String AVTALT = "AVTALT";
        public static final String FRADATO = "FRADATO";
        public static final String TILDATO = "TILDATO";
        public static final String OPPDATERTDATO = "OPPDATERTDATO";
        public static final String STATUS = "STATUS";
        public static final String VERSION = "VERSION";
    }

    public static final class VEDTAK {
        public static final String TABLE_NAME = "VEDTAKSTATUS_DATA";
        public static final String VEDTAKID = "VEDTAKID";
        public static final String VEDTAKSTATUS = "VEDTAKSTATUS";
        public static final String INNSATSGRUPPE = "INNSATSGRUPPE";
        public static final String HOVEDMAL = "HOVEDMAL";
        public static final String VEDTAK_STATUS_ENDRET_TIDSPUNKT = "VEDTAK_STATUS_ENDRET_TIDSPUNKT";
        public static final String AKTOERID = "AKTOERID";
        public static final String ANSVARLIG_VEILEDER_IDENT = "ANSVARLIG_VEILEDER_IDENT";
        public static final String ANSVARLIG_VEILEDER_NAVN = "ANSVARLIG_VEILEDER_NAVN";
    }

    public static final class PDL_DATA {
        public static final String TABLE_NAME = "PDL_DATA";
        public static final String AKTOERID = "AKTOERID";
        public static final String FODSELSDAG = "FODSELSDAG";
    }
}
