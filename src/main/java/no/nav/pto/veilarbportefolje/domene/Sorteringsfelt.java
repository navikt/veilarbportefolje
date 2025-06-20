package no.nav.pto.veilarbportefolje.domene;

/**
 * "Sorteringsfelt" tilsvarer (med få unntak) kolonner ein kan sortere på i frontend (veilarbportefoljeflatefs).
 * <p>
 * name – eksempel: IKKE_SATT, Sorteringsfelt.IKKE_SATT
 * - skildrar sorterinsgfeltet
 * - vert brukt i backendkoden
 * - tilsvarer stort sett namna på enumverdiane i frontend
 * - er notert med SCREAMING_SNAKE_CASE
 * - kan nåast i koden med .name() om ein treng hente den som tekst
 * <p>
 * sorteringsverdi - eksempel: "ikke_satt"
 * - verdien som blir sendt mellom backend og frontend, og brukt til å sortere resultata
 * - har ymse noteringsformat, oftast snake_case
 * - kan hentast ut ved .value
 */
public enum Sorteringsfelt {
    IKKE_SATT("ikke_satt"),
    VALGTE_AKTIVITETER("valgteaktiviteter"),
    ETTERNAVN("etternavn"),
    FODSELSNUMMER("fodselsnummer"),
    OPPFOLGING_STARTET("oppfolging_startdato"),
    UTLOPSDATO("utlopsdato"),

    VEILEDER_IDENT("veileder_id"),  // "NAVIDENT" i frontend

    DAGPENGER_UTLOP_UKE("dagputlopuke"),
    DAGPENGER_PERM_UTLOP_UKE("permutlopuke"),

    AAP_TYPE("aap_type"),
    AAP_VURDERINGSFRIST("aap_vurderingsfrist"),
    AAP_MAXTID_UKE("aapmaxtiduke"),
    AAP_UNNTAK_UKER_IGJEN("aapunntakukerigjen"),

    VENTER_PA_SVAR_FRA_NAV("venterpasvarfranav"),
    VENTER_PA_SVAR_FRA_BRUKER("venterpasvarfrabruker"),

    I_AVTALT_AKTIVITET("iavtaltaktivitet"),
    UTLOPTE_AKTIVITETER("utlopteaktiviteter"),
    STARTDATO_FOR_AVTALT_AKTIVITET("aktivitet_start"),
    NESTE_STARTDATO_FOR_AVTALT_AKTIVITET("neste_aktivitet_start"),
    FORRIGE_DATO_FOR_AVTALT_AKTIVITET("forrige_aktivitet_start"),

    AAP_RETTIGHETSPERIODE("aaprettighetsperiode"),

    MOTER_MED_NAV_IDAG("moterMedNAVIdag"),
    MOTESTATUS("motestatus"),

    GJELDENDE_VEDTAK_14A_INNSATSGRUPPE("gjeldende_vedtak_14a_innsatsgruppe"),
    GJELDENDE_VEDTAK_14A_HOVEDMAL("gjeldende_vedtak_14a_hovedmal"),
    GJELDENDE_VEDTAK_14A_VEDTAKSDATO("gjeldende_vedtak_14a_vedtaksdato"),

    UTKAST_14A_STATUS("utkast_14a_status"),
    UTKAST_14A_STATUS_ENDRET("utkast_14a_status_endret"),
    UTKAST_14A_ANSVARLIG_VEILEDER("utkast_14a_ansvarlig_veileder"),

    SISTE_ENDRING_DATO("siste_endring_tidspunkt"),

    FODELAND("fodeland"),
    STATSBORGERSKAP("statsborgerskap"),
    STATSBORGERSKAP_GYLDIG_FRA("statsborgerskap_gyldig_fra"),

    TOLKESPRAK("tolkespraak"),
    TOLKEBEHOV_SIST_OPPDATERT("tolkebehov_sistoppdatert"),

    BOSTED_KOMMUNE("kommunenummer"),
    BOSTED_BYDEL("bydelsnummer"),
    BOSTED_SIST_OPPDATERT("bostedSistOppdatert"),

    CV_SVARFRIST("neste_svarfrist_stilling_fra_nav"),

    ENSLIGE_FORSORGERE_UTLOP_YTELSE("enslige_forsorgere_utlop_ytelse"),
    ENSLIGE_FORSORGERE_VEDTAKSPERIODETYPE("enslige_forsorgere_vedtaksperiodetype"),
    ENSLIGE_FORSORGERE_AKTIVITETSPLIKT("enslige_forsorgere_aktivitetsplikt"),
    ENSLIGE_FORSORGERE_OM_BARNET("enslige_forsorgere_om_barnet"),

    BARN_UNDER_18_AR("barn_under_18_aar"),

    BRUKERS_SITUASJON_SIST_ENDRET("brukersSituasjonSistEndret"),
    UTDANNING_OG_SITUASJON_SIST_ENDRET("utdanningOgSituasjonSistEndret"),

    HUSKELAPP_KOMMENTAR("huskelapp_kommentar"),
    HUSKELAPP_FRIST("huskelapp_frist"),
    HUSKELAPP("huskelapp"),

    FARGEKATEGORI("fargekategori"),

    TILTAKSHENDELSE_TEKST("tiltakshendelse_tekst"),
    TILTAKSHENDELSE_DATO_OPPRETTET("tiltakshendelse_dato_opprettet"),

    UTGATT_VARSEL_DATO("filterhendelse_dato_opprettet");

    /**
     * Filterverdien som vert sendt mellom frontend og backend
     */
    public final String sorteringsverdi;

    Sorteringsfelt(String sorteringsverdi) {
        this.sorteringsverdi = sorteringsverdi;
    }

    public static Sorteringsfelt toSorteringsfelt(String sorteringsverdi) {
        for (Sorteringsfelt sorteringsfelt : values()) {
            if (sorteringsfelt.sorteringsverdi.equals(sorteringsverdi)) {
                return sorteringsfelt;
            }
        }
        throw new IllegalArgumentException("Ugyldig verdi for enum: " + sorteringsverdi);
    }

    @Override
    public String toString() {
        return this.name() + " (" + this.sorteringsverdi + ")";
    }
}
