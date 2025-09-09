package no.nav.pto.veilarbportefolje.database;

public class PostgresTable {
    public static final class OPENSEARCHDATA {

        private OPENSEARCHDATA() { /* no-op */ }

        // OPPFOLGING_DATA
        public static final String OPPFOLGING_DATA_AKTOERID = "OPPFOLGING_DATA_AKTOERID";
        public static final String OPPFOLGING_DATA_STARTDATO = "OPPFOLGING_DATA_STARTDATO";
        public static final String OPPFOLGING_DATA_NY_FOR_VEILEDER = "OPPFOLGING_DATA_NY_FOR_VEILEDER";
        public static final String OPPFOLGING_DATA_VEILEDERID = "OPPFOLGING_DATA_VEILEDERID";
        public static final String OPPFOLGING_DATA_MANUELL = "OPPFOLGING_DATA_MANUELL";
        public static final String OPPFOLGING_DATA_OPPFOLGING = "OPPFOLGING_DATA_OPPFOLGING";

        // AKTIVE_IDENTER
        public static final String AKTIVE_IDENTER_FNR = "AKTIVE_IDENTER_FNR";

        // OPPFOLGINGSBRUKER_ARENA_V2
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR = "OPPFOLGINGSBRUKER_ARENA_V2_FODSELSNR";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_FORMIDLINGSGRUPPEKODE = "OPPFOLGINGSBRUKER_ARENA_V2_FORMIDLINGSGRUPPEKODE";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_ISERV_FRA_DATO = "OPPFOLGINGSBRUKER_ARENA_V2_ISERV_FRA_DATO";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_NAV_KONTOR = "OPPFOLGINGSBRUKER_ARENA_V2_NAV_KONTOR";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE = "OPPFOLGINGSBRUKER_ARENA_V2_KVALIFISERINGSGRUPPEKODE";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_RETTIGHETSGRUPPEKODE = "OPPFOLGINGSBRUKER_ARENA_V2_RETTIGHETSGRUPPEKODE";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_HOVEDMAALKODE = "OPPFOLGINGSBRUKER_ARENA_V2_HOVEDMAALKODE";
        public static final String OPPFOLGINGSBRUKER_ARENA_V2_ENDRET_DATO = "OPPFOLGINGSBRUKER_ARENA_V2_ENDRET_DATO";

        // NOM_SKJERMING
        public static final String NOM_SKJERMING_ER_SKJERMET = "NOM_SKJERMING_ER_SKJERMET";
        public static final String NOM_SKJERMING_SKJERMET_TIL = "NOM_SKJERMING_SKJERMET_TIL";

        // BRUKER_DATA
        public static final String BRUKER_DATA_FOEDSELSDATO = "BRUKER_DATA_FOEDSELSDATO";
        public static final String BRUKER_DATA_FORNAVN = "BRUKER_DATA_FORNAVN";
        public static final String BRUKER_DATA_MELLOMNAVN = "BRUKER_DATA_MELLOMNAVN";
        public static final String BRUKER_DATA_ETTERNAVN = "BRUKER_DATA_ETTERNAVN";
        public static final String BRUKER_DATA_ER_DOED = "BRUKER_DATA_ER_DOED";
        public static final String BRUKER_DATA_KJOENN = "BRUKER_DATA_KJOENN";
        public static final String BRUKER_DATA_FOEDELAND = "BRUKER_DATA_FOEDELAND";
        public static final String BRUKER_DATA_TALESPRAAKTOLK = "BRUKER_DATA_TALESPRAAKTOLK";
        public static final String BRUKER_DATA_TEGNSPRAAKTOLK = "BRUKER_DATA_TEGNSPRAAKTOLK";
        public static final String BRUKER_DATA_TOLKBEHOVSISTOPPDATERT = "BRUKER_DATA_TOLKBEHOVSISTOPPDATERT";
        public static final String BRUKER_DATA_DISKRESJONKODE = "BRUKER_DATA_DISKRESJONKODE";
        public static final String BRUKER_DATA_SIKKERHETSTILTAK_TYPE = "BRUKER_DATA_SIKKERHETSTILTAK_TYPE";
        public static final String BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGFRA = "BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGFRA";
        public static final String BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGTIL = "BRUKER_DATA_SIKKERHETSTILTAK_GYLDIGTIL";
        public static final String BRUKER_DATA_SIKKERHETSTILTAK_BESKRIVELSE = "BRUKER_DATA_SIKKERHETSTILTAK_BESKRIVELSE";
        public static final String BRUKER_DATA_BYDELSNUMMER = "BRUKER_DATA_BYDELSNUMMER";
        public static final String BRUKER_DATA_KOMMUNENUMMER = "BRUKER_DATA_KOMMUNENUMMER";
        public static final String BRUKER_DATA_BOSTEDSISTOPPDATERT = "BRUKER_DATA_BOSTEDSISTOPPDATERT";
        public static final String BRUKER_DATA_UTENLANDSKADRESSE = "BRUKER_DATA_UTENLANDSKADRESSE";
        public static final String BRUKER_DATA_HARUKJENTBOSTED = "BRUKER_DATA_HARUKJENTBOSTED";

        // DIALOG
        public static final String DIALOG_VENTER_PA_BRUKER = "DIALOG_VENTER_PA_BRUKER";
        public static final String DIALOG_VENTER_PA_NAV = "DIALOG_VENTER_PA_NAV";

        // UTKAST_14A_STATUS
        public static final String UTKAST_14A_STATUS_VEDTAKSTATUS = "UTKAST_14A_STATUS_VEDTAKSTATUS";
        public static final String UTKAST_14A_STATUS_ANSVARLIG_VEILDERNAVN = "UTKAST_14A_STATUS_ANSVARLIG_VEILDERNAVN";
        public static final String UTKAST_14A_STATUS_ENDRET_TIDSPUNKT = "UTKAST_14A_STATUS_ENDRET_TIDSPUNKT";

        // ARBEIDSLISTE
        public static final String ARBEIDSLISTE_SIST_ENDRET_AV_VEILEDERIDENT = "ARBEIDSLISTE_SIST_ENDRET_AV_VEILEDERIDENT";
        public static final String ARBEIDSLISTE_ENDRINGSTIDSPUNKT = "ARBEIDSLISTE_ENDRINGSTIDSPUNKT";
        public static final String ARBEIDSLISTE_OVERSKRIFT = "ARBEIDSLISTE_OVERSKRIFT";
        public static final String ARBEIDSLISTE_FRIST = "ARBEIDSLISTE_FRIST";
        public static final String ARBEIDSLISTE_KATEGORI = "ARBEIDSLISTE_KATEGORI";
        public static final String ARBEIDSLISTE_NAV_KONTOR_FOR_ARBEIDSLISTE = "ARBEIDSLISTE_NAV_KONTOR_FOR_ARBEIDSLISTE";

        // BRUKER_PROFILERING
        public static final String BRUKER_PROFILERING_PROFILERING_RESULTAT = "BRUKER_PROFILERING_PROFILERING_RESULTAT";

        // BRUKER_CV
        public static final String BRUKER_CV_HAR_DELT_CV = "BRUKER_CV_HAR_DELT_CV";
        public static final String BRUKER_CV_CV_EKSISTERER = "BRUKER_CV_CV_EKSISTERER";

        // BRUKER_REGISTRERING
        public static final String BRUKER_REGISTRERING_BRUKERS_SITUASJON = "BRUKER_REGISTRERING_BRUKERS_SITUASJON";
        public static final String BRUKER_REGISTRERING_REGISTRERING_OPPRETTET = "BRUKER_REGISTRERING_REGISTRERING_OPPRETTET";
        public static final String BRUKER_REGISTRERING_UTDANNING = "BRUKER_REGISTRERING_UTDANNING";
        public static final String BRUKER_REGISTRERING_UTDANNING_BESTATT = "BRUKER_REGISTRERING_UTDANNING_BESTATT";
        public static final String BRUKER_REGISTRERING_UTDANNING_GODKJENT = "BRUKER_REGISTRERING_UTDANNING_GODKJENT";

        // YTELSE_STATUS_FOR_BRUKER
        public static final String YTELSE_STATUS_FOR_BRUKER_YTELSE = "YTELSE_STATUS_FOR_BRUKER_YTELSE";
        public static final String YTELSE_STATUS_FOR_BRUKER_AAPMAXTIDUKE = "YTELSE_STATUS_FOR_BRUKER_AAPMAXTIDUKE";
        public static final String YTELSE_STATUS_FOR_BRUKER_AAPUNNTAKDAGERIGJEN = "YTELSE_STATUS_FOR_BRUKER_AAPUNNTAKDAGERIGJEN";
        public static final String YTELSE_STATUS_FOR_BRUKER_DAGPUTLOPUKE = "YTELSE_STATUS_FOR_BRUKER_DAGPUTLOPUKE";
        public static final String YTELSE_STATUS_FOR_BRUKER_PERMUTLOPUKE = "YTELSE_STATUS_FOR_BRUKER_PERMUTLOPUKE";
        public static final String YTELSE_STATUS_FOR_BRUKER_UTLOPSDATO = "YTELSE_STATUS_FOR_BRUKER_UTLOPSDATO";
        public static final String YTELSE_STATUS_FOR_BRUKER_ANTALLDAGERIGJEN = "YTELSE_STATUS_FOR_BRUKER_ANTALLDAGERIGJEN";
        public static final String YTELSE_STATUS_FOR_BRUKER_ENDRET_DATO = "YTELSE_STATUS_FOR_BRUKER_ENDRET_DATO";

        // ENDRING_I_REGISTRERING
        public static final String ENDRING_I_REGISTRERING_BRUKERS_SITUASJON = "ENDRING_I_REGISTRERING_BRUKERS_SITUASJON";
        public static final String ENDRING_I_REGISTRERING_BRUKERS_SITUASJON_SIST_ENDRET = "ENDRING_I_REGISTRERING_BRUKERS_SITUASJON_SIST_ENDRET";

        // FARGEKATEGORI
        public static final String FARGEKATEGORI_VERDI = "FARGEKATEGORI_VERDI";
        public static final String FARGEKATEGORI_ENHET_ID = "FARGEKATEGORI_ENHET_ID";

        // HUSKELAPP
        public static final String HUSKELAPP_FRIST = "HUSKELAPP_FRIST";
        public static final String HUSKELAPP_KOMMENTAR = "HUSKELAPP_KOMMENTAR";
        public static final String HUSKELAPP_ENDRET_DATO = "HUSKELAPP_ENDRET_DATO";
        public static final String HUSKELAPP_ENDRET_AV_VEILEDER = "HUSKELAPP_ENDRET_AV_VEILEDER";
        public static final String HUSKELAPP_HUSKELAPP_ID = "HUSKELAPP_HUSKELAPP_ID";
        public static final String HUSKELAPP_ENHET_ID = "HUSKELAPP_ENHET_ID";
    }

    public static final class OPPFOLGING_DATA {

        private OPPFOLGING_DATA() { /* no-op */ }

        public static final String TABLE_NAME = "OPPFOLGING_DATA";

        public static final String AKTOERID = "AKTOERID";
        public static final String VEILEDERID = "VEILEDERID";
        public static final String OPPFOLGING = "OPPFOLGING";
        public static final String NY_FOR_VEILEDER = "NY_FOR_VEILEDER";
        public static final String MANUELL = "MANUELL";
        public static final String STARTDATO = "STARTDATO";
    }

    public static final class OPPFOLGINGSBRUKER_ARENA_V2 {

        private OPPFOLGINGSBRUKER_ARENA_V2() { /* no-op */ }

        public static final String TABLE_NAME = "OPPFOLGINGSBRUKER_ARENA_V2";
        public static final String FODSELSNR = "FODSELSNR";
        public static final String FORMIDLINGSGRUPPEKODE = "FORMIDLINGSGRUPPEKODE";
        public static final String ISERV_FRA_DATO = "ISERV_FRA_DATO";
        public static final String NAV_KONTOR = "NAV_KONTOR";
        public static final String KVALIFISERINGSGRUPPEKODE = "KVALIFISERINGSGRUPPEKODE";
        public static final String RETTIGHETSGRUPPEKODE = "RETTIGHETSGRUPPEKODE";
        public static final String HOVEDMAALKODE = "HOVEDMAALKODE";
        public static final String ENDRET_DATO = "ENDRET_DATO";
    }

    public static final class DIALOG {

        private DIALOG() { /* no-op */ }

        public static final String TABLE_NAME = "DIALOG";

        public static final String AKTOERID = "AKTOERID";
        public static final String VENTER_PA_BRUKER = "VENTER_PA_BRUKER";
        public static final String VENTER_PA_NAV = "VENTER_PA_NAV";
    }

    public static final class ARBEIDSLISTE {

        private ARBEIDSLISTE() { /* no-op */ }

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

    public static final class FARGEKATEGORI {

        private FARGEKATEGORI() { /* no-op */ }

        public static final String ID = "ID";
        public static final String FNR = "FNR";
        public static final String VERDI = "VERDI";
        public static final String SIST_ENDRET = "SIST_ENDRET";
        public static final String SIST_ENDRET_AV_VEILEDERIDENT = "SIST_ENDRET_AV_VEILEDERIDENT";
        public static final String ENHET_ID = "ENHET_ID";
    }

    public static final class HUSKELAPP {

        private HUSKELAPP() { /* no-op */ }

        public static final String TABLE_NAME = "HUSKELAPP";
        public static final String ENDRINGS_ID = "ENDRINGS_ID";
        public static final String HUSKELAPP_ID = "HUSKELAPP_ID";
        public static final String FNR = "FNR";
        public static final String ENHET_ID = "ENHET_ID";
        public static final String ENDRET_AV_VEILEDER = "ENDRET_AV_VEILEDER";
        public static final String ENDRET_DATO = "ENDRET_DATO";
        public static final String FRIST = "FRIST";
        public static final String KOMMENTAR = "KOMMENTAR";
        public static final String STATUS = "STATUS";
    }

    public static final class UTKAST_14A_STATUS {

        private UTKAST_14A_STATUS() { /* no-op */ }

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

        private LEST_ARENA_HENDELSE_AKTIVITETER() { /* no-op */ }

        public static final String TABLE_NAME = "LEST_ARENA_HENDELSE_AKTIVITET";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String HENDELSE_ID = "HENDELSE_ID";
    }

    public static final class YTELSESVEDTAK {

        private YTELSESVEDTAK() { /* no-op */ }

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
        public static final String ANTALLDAGERIGJENUNNTAK = "ANTALLDAGERIGJENUNNTAK";
        public static final String ANTALLDAGERIGJEN = "ANTALLDAGERIGJEN";
        public static final String ENDRET_DATO = "ENDRET_DATO";
    }

    public static final class LEST_ARENA_HENDELSE_YTELSER {

        private LEST_ARENA_HENDELSE_YTELSER() { /* no-op */ }

        public static final String TABLE_NAME = "LEST_ARENA_HENDELSE_YTELSE";
        public static final String VEDTAKID = "VEDTAKID";
        public static final String HENDELSE_ID = "HENDELSE_ID";
    }

    public static final class BRUKER_PROFILERING {

        private BRUKER_PROFILERING() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKER_PROFILERING";

        public static final String AKTOERID = "AKTOERID";
        public static final String PROFILERING_RESULTAT = "PROFILERING_RESULTAT";
        public static final String PROFILERING_TIDSPUNKT = "PROFILERING_TIDSPUNKT";
    }

    public static final class BRUKER_REGISTRERING {

        private BRUKER_REGISTRERING() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKER_REGISTRERING";

        public static final String AKTOERID = "AKTOERID";
        public static final String BRUKERS_SITUASJON = "BRUKERS_SITUASJON";
        public static final String REGISTRERING_OPPRETTET = "REGISTRERING_OPPRETTET";
        public static final String UTDANNING = "UTDANNING";
        public static final String UTDANNING_BESTATT = "UTDANNING_BESTATT";
        public static final String UTDANNING_GODKJENT = "UTDANNING_GODKJENT";
    }

    public static final class OPPLYSNINGER_OM_ARBEIDSSOEKER {
        private OPPLYSNINGER_OM_ARBEIDSSOEKER() { /* no-op */ }

        public static final String TABLE_NAME = "OPPLYSNINGER_OM_ARBEIDSSOEKER";
        public static final String OPPLYSNINGER_OM_ARBEIDSSOEKER_ID = "OPPLYSNINGER_OM_ARBEIDSSOEKER_ID";
        public static final String PERIODE_ID = "PERIODE_ID";
        public static final String SENDT_INN_TIDSPUNKT = "SENDT_INN_TIDSPUNKT";
        public static final String UTDANNING_NUS_KODE = "UTDANNING_NUS_KODE";
        public static final String UTDANNING_BESTATT = "UTDANNING_BESTATT";
        public static final String UTDANNING_GODKJENT = "UTDANNING_GODKJENT";

    }

    public static final class OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON {
        private OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON() { /* no-op */ }

        public static final String TABLE_NAME = "OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON";
        public static final String OPPLYSNINGER_OM_ARBEIDSSOEKER_ID = "OPPLYSNINGER_OM_ARBEIDSSOEKER_ID";
        public static final String JOBBSITUASJON = "JOBBSITUASJON";

    }

    public static final class SISTE_ARBEIDSSOEKER_PERIODE {
        private SISTE_ARBEIDSSOEKER_PERIODE() { /* no-op */ }

        public static final String TABLE_NAME = "SISTE_ARBEIDSSOEKER_PERIODE";
        public static final String ARBEIDSSOKER_PERIODE_ID = "ARBEIDSSOKER_PERIODE_ID";
        public static final String FNR = "FNR";

    }

    public static final class PROFILERING {
        private PROFILERING() { /* no-op */ }

        public static final String TABLE_NAME = "PROFILERING";
        public static final String PERIODE_ID = "PERIODE_ID";
        public static final String PROFILERING_RESULTAT = "PROFILERING_RESULTAT";
        public static final String SENDT_INN_TIDSPUNKT = "SENDT_INN_TIDSPUNKT";

    }

    public static final class BRUKER_CV {

        private BRUKER_CV() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKER_CV";

        public static final String AKTOERID = "AKTOERID";
        public static final String HAR_DELT_CV = "HAR_DELT_CV";
        public static final String SISTE_MELDING_MOTTATT = "SISTE_MELDING_MOTTATT";
        public static final String CV_EKSISTERER = "CV_EKSISTERER";
    }

    public static final class GRUPPE_AKTIVITER {

        private GRUPPE_AKTIVITER() { /* no-op */ }

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

        private AKTIVITETER() { /* no-op */ }

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

        private BRUKERTILTAK() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKERTILTAK";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String PERSONID = "PERSONID";
        public static final String TILTAKSKODE = "TILTAKSKODE";
        public static final String TILDATO = "TILDATO";
        public static final String FRADATO = "FRADATO";
    }

    public static final class BRUKERTILTAK_V2 {

        private BRUKERTILTAK_V2() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKERTILTAK_V2";
        public static final String AKTIVITETID = "AKTIVITETID";
        public static final String AKTOERID = "AKTOERID";
        public static final String TILTAKSKODE = "TILTAKSKODE";
        public static final String TILDATO = "TILDATO";
        public static final String FRADATO = "FRADATO";
        public static final String VERSION = "VERSION";
    }

    public static final class TILTAKKODEVERK {

        private TILTAKKODEVERK() { /* no-op */ }

        public static final String TABLE_NAME = "TILTAKKODEVERKET";
        public static final String KODE = "KODE";
        public static final String VERDI = "VERDI";
    }

    public static final class NOM_SKJERMING {

        private NOM_SKJERMING() { /* no-op */ }

        public static final String TABLE_NAME = "NOM_SKJERMING";
        public static final String FNR = "FODSELSNR";
        public static final String ER_SKJERMET = "ER_SKJERMET";
        public static final String SKJERMET_FRA = "SKJERMET_FRA";
        public static final String SKJERMET_TIL = "SKJERMET_TIL";
    }

    public static final class SISTE_ENDRING {

        private SISTE_ENDRING() { /* no-op */ }

        public static final String TABLE_NAME = "SISTE_ENDRING";
        public static final String AKTOERID = "AKTOERID";
        public static final String AKTIVITETID = "AKTIVITETID";

        public static final String SISTE_ENDRING_KATEGORI = "SISTE_ENDRING_KATEGORI";
        public static final String SISTE_ENDRING_TIDSPUNKT = "SISTE_ENDRING_TIDSPUNKT";
        public static final String ER_SETT = "ER_SETT";
    }

    public static final class BRUKER_STATSBORGERSKAP {

        private BRUKER_STATSBORGERSKAP() { /* no-op */ }

        public static final String TABLE_NAME = "BRUKER_STATSBORGERSKAP";
        public static final String FNR = "FREG_IDENT";
        public static final String STATSBORGERSKAP = "STATSBORGERSKAP";
        public static final String GYLDIG_FRA = "GYLDIG_FRA";
        public static final String GYLDIG_TIL = "GYLDIG_TIL";
    }

    public static final class TILTAKSHENDELSE {
        private TILTAKSHENDELSE() { /* no-op*/ }

        public static final String ID = "ID";
        public static final String FNR = "FNR";
        public static final String OPPRETTET = "OPPRETTET";
        public static final String TEKST = "TEKST";
        public static final String LENKE = "LENKE";
        public static final String TILTAKSTYPE = "TILTAKSTYPE_KODE";
        public static final String AVSENDER = "AVSENDER";
        public static final String SIST_ENDRET = "SIST_ENDRET";
    }

    public static final class HENDELSE {
        private HENDELSE() { /* no-op */ }

        public static final String TABLE_NAME = "FILTERHENDELSER";
        public static final String ID = "ID";
        public static final String PERSON_IDENT = "PERSON_IDENT";
        public static final String HENDELSE_NAVN = "HENDELSE_NAVN";
        public static final String HENDELSE_DATO = "HENDELSE_DATO";
        public static final String HENDELSE_LENKE = "HENDELSE_LENKE";
        public static final String HENDELSE_DETALJER = "HENDELSE_DETALJER";
        public static final String KATEGORI = "KATEGORI";
        public static final String AVSENDER = "AVSENDER";
        public static final String OPPRETTET = "OPPRETTET";
        public static final String SIST_ENDRET = "SIST_ENDRET";
    }

    public static final class YTELSER_AAP {
        private YTELSER_AAP() { /* no-op */ }

        public static final String TABLE_NAME = "YTELSER_AAP";
        public static final String NORSK_IDENT = "NORSK_IDENT";
        public static final String STATUS = "STATUS";
        public static final String SAKSID = "SAKSID";
        public static final String NYESTE_PERIODE_FOM = "NYESTE_PERIODE_FOM";
        public static final String NYESTE_PERIODE_TOM = "NYESTE_PERIODE_TOM";
        public static final String RETTIGHETSTYPE = "RETTIGHETSTYPE";
        public static final String OPPHORSAARSAK = "OPPHORSAARSAK";
        public static final String RAD_SIST_ENDRET = "RAD_SIST_ENDRET";
    }

}
