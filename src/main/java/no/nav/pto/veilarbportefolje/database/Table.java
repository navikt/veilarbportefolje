package no.nav.pto.veilarbportefolje.database;

public class Table {
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
        public static final String AKTOERID = "AKTOERID";
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
        public static final String OPPFOLGING = "OPPFOLGING";

    }

    public static final class BRUKER_DATA {
        public static final String TABLE_NAME = "BRUKER_DATA";
        public static final String PERSONID = "PERSONID";
        public static final String AKTOERID = "AKTOERID";

        public static final String NYESTEUTLOPTEAKTIVITET = "NYESTEUTLOPTEAKTIVITET";
        public static final String AKTIVITET_START = "AKTIVITET_START";
        public static final String NESTE_AKTIVITET_START = "NESTE_AKTIVITET_START";
        public static final String FORRIGE_AKTIVITET_START = "FORRIGE_AKTIVITET_START";
    }

    public static final class BRUKER_CV {
        public static final String TABLE_NAME = "BRUKER_CV";

        public static final String AKTOERID = "AKTOERID";
        public static final String HAR_DELT_CV = "HAR_DELT_CV";
        public static final String CV_EKSISTERE = "CV_EKSISTERE";
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

    @Deprecated
    public static final class TILTAKKODEVERK {
        public static final String TABLE_NAME = "tiltakkodeverk";
        public static final String KODE = "kode";
        public static final String VERDI = "verdi";
    }

    public static final class TILTAKKODEVERK_V2 {
        public static final String TABLE_NAME = "TILTAKKODEVERKET_V2";
        public static final String KODE = "KODE";
        public static final String VERDI = "VERDI";
    }

    @Deprecated
    public static final class ENHETTILTAK {
        public static final String TABLE_NAME = "ENHETTILTAK";
        public static final String ENHETID = "ENHETID";
        public static final String TILTAKSKODE = "TILTAKSKODE";
    }

    public static final class BRUKERTILTAK_V2 {
        public static final String TABLE_NAME = "BRUKERTILTAK_V2";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSONID = "PERSONID";
        public static final String TILTAKSKODE = "TILTAKSKODE";
        public static final String TILDATO = "TILDATO";
        public static final String FRADATO = "FRADATO";
    }

    public static final class HOVEDINDEKSERING {
        public static final String TABLE_NAME = "HOVEDINDEKSERING";
        public static final String ID = "ID";
        public static final String SISTE_HOVEDINDEKSERING_TIDSPUNKT = "SISTE_HOVEDINDEKSERING_TIDSPUNKT";
    }

    public static final class YTELSER {
        public static final String TABLE_NAME = "YTELSER";
        public static final String VEDTAKID = "VEDTAKID";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSONID = "PERSONID";
        public static final String YTELSESTYPE = "YTELSESTYPE";
        public static final String SAKSID = "SAKSID";

        public static final String SAKSTYPEKODE = "SAKSTYPEKODE";
        public static final String RETTIGHETSTYPEKODE = "RETTIGHETSTYPEKODE";

        public static final String STARTDATO = "STARTDATO";
        public static final String UTLOPSDATO = "UTLOPSDATO";
        public static final String ANTALLUKERIGJEN = "ANTALLUKERIGJEN";
        public static final String ANTALLPERMITTERINGUKER = "ANTALLPERMITTERINGUKER";
        public static final String ANTALLUKERIGJENUNNTAK = "ANTALLUKERIGJENUNNTAK";
    }

    public static final class KAFKA_CONSUMER_RECORD {
        public static final String TABLE_NAME = "KAFKA_CONSUMER_RECORD";
        public static final String ID = "ID";
        public static final String RETRIES = "RETRIES";
        public static final String CREATED_AT = "CREATED_AT";
        public static final String LAST_RETRY = "LAST_RETRY";
    }
}
