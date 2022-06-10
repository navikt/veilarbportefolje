package no.nav.pto.veilarbportefolje.database;

public class PostgresTable {
    public static final class OpensearchData {

        public static final String FODSELSNR = "FNR";
        public static final String FODSELSNR_ARENA = "fodselsnr";

        public static final String AKTOERID = "AKTOERID";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String STARTDATO = "STARTDATO";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
        public static final String VEILEDERID = "VEILEDERID";
        public static final String MANUELL = "MANUELL";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
        public static final String UTKAST_14A_STATUS = "VEDTAKSTATUS";
        public static final String UTKAST_14A_ANSVARLIG_VEILDERNAVN = "VEDTAKSTATUS_ANSVARLIG_VEILDERNAVN";
        public static final String UTKAST_14A_ENDRET_TIDSPUNKT = "VEDTAKSTATUS_ENDRET_TIDSPUNKT";
        public static final String PROFILERING_RESULTAT = "PROFILERING_RESULTAT";
        public static final String HAR_DELT_CV = "HAR_DELT_CV";
        public static final String CV_EKSISTERER = "CV_EKSISTERER";

        public static final String ARB_SIST_ENDRET_AV_VEILEDERIDENT = "ARB_SIST_ENDRET_AV_VEILEDERIDENT";
        public static final String ARB_ENDRINGSTIDSPUNKT = "ARB_ENDRINGSTIDSPUNKT";
        public static final String ARB_FRIST = "ARB_FRIST";
        public static final String ARB_KATEGORI = "ARB_KATEGORI";
        public static final String ARB_OVERSKRIFT = "ARB_OVERSKRIFT";
        public static final String BRUKERS_SITUASJON = "BRUKERS_SITUASJON";
        public static final String UTDANNING = "UTDANNING";
        public static final String UTDANNING_BESTATT = "UTDANNING_BESTATT";
        public static final String UTDANNING_GODKJENT = "UTDANNING_GODKJENT";

        public static final String YTELSE = "YTELSE";
        public static final String AAPMAXTIDUKE = "AAPMAXTIDUKE";
        public static final String AAPUNNTAKDAGERIGJEN = "AAPUNNTAKDAGERIGJEN";
        public static final String DAGPUTLOPUKE = "DAGPUTLOPUKE";
        public static final String PERMUTLOPUKE = "PERMUTLOPUKE";
        public static final String YTELSE_UTLOPSDATO = "YTELSE_UTLOPSDATO";

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
        public static final String SPERRET_ANSATT_ARENA = "SPERRET_ANSATT";
        public static final String ER_DOED = "ER_DOED";

        public static final String ER_SKJERMET = "ER_SKJERMET";
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

    public static final class OPPFOLGINGSBRUKER_ARENA_V2 {
        public static final String TABLE_NAME = "OPPFOLGINGSBRUKER_ARENA";
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
        public static final String SPERRET_ANSATT = "SPERRET_ANSATT";
        public static final String ER_DOED = "ER_DOED";
        public static final String ENDRET_DATO = "ENDRET_DATO";
    }

    public static final class DIALOG {
        public static final String TABLE_NAME = "DIALOG";

        public static final String AKTOERID = "AKTOERID";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
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
    }

    public static final class UTKAST_14A_STATUS {
        public static final String TABLE_NAME = "UTKAST_14A_STATUS";

        public static final String AKTOERID = "AKTOERID";
        public static final String ID = "VEDTAKID";
        public static final String STATUS = "VEDTAKSTATUS";
        public static final String INNSATSGRUPPE = "INNSATSGRUPPE";
        public static final String HOVEDMAL = "HOVEDMAL";
        public static final String ANSVARLIG_VEILDERIDENT = "ANSVARLIG_VEILDERIDENT";
        public static final String ANSVARLIG_VEILDERNAVN = "ANSVARLIG_VEILDERNAVN";
        public static final String ENDRET_TIDSPUNKT = "ENDRET_TIDSPUNKT";
    }

    public static final class LEST_ARENA_HENDELSE_AKTIVITETER {
        public static final String TABLE_NAME = "LEST_ARENA_HENDELSE_AKTIVITET";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String HENDELSE_ID = "HENDELSE_ID";
    }

    public static final class YTELSESVEDTAK {
        public static final String TABLE_NAME = "YTELSESVEDTAK";

        public static final String VEDTAKSID = "VEDTAKSID";
        public static final String AKTORID = "AKTORID";
        public static final String PERSONID = "PERSONID";
        public static final String YTELSESTYPE = "YTELSESTYPE";
        public static final String SAKSID = "SAKSID";
        public static final String SAKSTYPEKODE = "SAKSTYPEKODE";
        public static final String RETTIGHETSTYPEKODE = "RETTIGHETSTYPEKODE";
        public static final String STARTDATO = "STARTDATO";
        public static final String UTLOPSDATO = "UTLOPSDATO";
        public static final String ANTALLUKERIGJEN = "ANTALLUKERIGJEN";
        public static final String ANTALLPERMITTERINGSUKER = "ANTALLPERMITTERINGSUKER";
        public static final String ANTALLUKERIGJENUNNTAK = "ANTALLUKERIGJENUNNTAK";
    }

    public static final class LEST_ARENA_HENDELSE_YTELSER {
        public static final String TABLE_NAME = "LEST_ARENA_HENDELSE_YTELSE";
        public static final String VEDTAKID = "VEDTAKID";
        public static final String HENDELSE_ID = "HENDELSE_ID";
    }

    public static final class BRUKER_PROFILERING {
        public static final String TABLE_NAME = "BRUKER_PROFILERING";

        public static final String AKTOERID = "AKTOERID";
        public static final String PROFILERING_RESULTAT = "PROFILERING_RESULTAT";
        public static final String PROFILERING_TIDSPUNKT = "PROFILERING_TIDSPUNKT";
    }

    public static final class BRUKER_REGISTRERING {
        public static final String TABLE_NAME = "BRUKER_REGISTRERING";

        public static final String AKTOERID = "AKTOERID";
        public static final String BRUKERS_SITUASJON = "BRUKERS_SITUASJON";
        public static final String REGISTRERING_OPPRETTET = "REGISTRERING_OPPRETTET";
        public static final String UTDANNING = "UTDANNING";
        public static final String UTDANNING_BESTATT = "UTDANNING_BESTATT";
        public static final String UTDANNING_GODKJENT = "UTDANNING_GODKJENT";
    }

    public static final class BRUKER_CV {
        public static final String TABLE_NAME = "BRUKER_CV";

        public static final String AKTOERID = "AKTOERID";
        public static final String HAR_DELT_CV = "HAR_DELT_CV";
        public static final String SISTE_MELDING_MOTTATT = "SISTE_MELDING_MOTTATT";
        public static final String CV_EKSISTERER = "CV_EKSISTERER";
    }

    public static final class GRUPPE_AKTIVITER {
        public static final String TABLE_NAME = "GRUPPE_AKTIVITER";
        public static final String MOTEPLAN_ID = "MOTEPLAN_ID";
        public static final String VEILEDNINGDELTAKER_ID = "VEILEDNINGDELTAKER_ID";

        public static final String AKTOERID = "AKTOERID";
        public static final String MOTEPLAN_STARTDATO = "MOTEPLAN_STARTDATO";
        public static final String MOTEPLAN_SLUTTDATO = "MOTEPLAN_SLUTTDATO";
        public static final String HENDELSE_ID = "HENDELSE_ID";
        public static final String AKTIV = "AKTIV";
    }

    public static final class AKTIVITETER {
        public static final String TABLE_NAME = "AKTIVITETER";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String AKTIVITETTYPE = "AKTIVITETTYPE";
        public static final String AVTALT = "AVTALT";
        public static final String FRADATO = "FRADATO";
        public static final String TILDATO = "TILDATO";
        public static final String STATUS = "STATUS";
        public static final String VERSION = "VERSION";
    }

    public static final class BRUKERTILTAK {
        public static final String TABLE_NAME = "BRUKERTILTAK";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSONID = "PERSONID";
        public static final String TILTAKSKODE = "TILTAKSKODE";
        public static final String TILDATO = "TILDATO";
        public static final String FRADATO = "FRADATO";
    }

    public static final class TILTAKKODEVERK {
        public static final String TABLE_NAME = "TILTAKKODEVERKET";
        public static final String KODE = "KODE";
        public static final String VERDI = "VERDI";
    }

    public static final class NOM_SKJERMING {
        public static final String TABLE_NAME = "NOM_SKJERMING";
        public static final String FNR = "FODSELSNR";
        public static final String ER_SKJERMET = "ER_SKJERMET";
        public static final String SKJERMET_FRA = "SKJERMET_FRA";
        public static final String SKJERMET_TIL = "SKJERMET_TIL";
    }

    public static final class SISTE_ENDRING {
        public static final String TABLE_NAME = "SISTE_ENDRING";
        public static final String AKTOERID = "AKTOERID";
        public static final String AKTIVITETID = "AKTIVITETID";

        public static final String SISTE_ENDRING_KATEGORI = "SISTE_ENDRING_KATEGORI";
        public static final String SISTE_ENDRING_TIDSPUNKT = "SISTE_ENDRING_TIDSPUNKT";
        public static final String ER_SETT = "ER_SETT";
    }

    public static final class BRUKER_STATSBORGERSKAP {
        public static final String TABLE_NAME = "BRUKER_STATSBORGERSKAP";
        public static final String FNR = "FREG_IDENT";
        public static final String STATSBORGERSKAP = "STATSBORGERSKAP";

        public static final String GYLDIG_FRA = "GYLDIG_FRA";
        public static final String GYLDIG_TIL = "GYLDIG_TIL";
    }
}
