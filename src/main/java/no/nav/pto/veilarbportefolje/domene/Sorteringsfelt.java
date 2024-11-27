package no.nav.pto.veilarbportefolje.domene;

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

    // TODO: Vi har andre enumar som heiter I_AVTALT_AKTIVITET og ein del andre brukarstatusar, bør namna her seie at dei er filtervarianten?
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

    ARBEIDSLISTE_FRIST("arbeidslistefrist"),
    ARBEIDSLISTE_KATEGORI("arbeidslistekategori"),
    ARBEIDSLISTE_OVERSKRIFT("arbeidsliste_overskrift"),

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
    TILTAKSHENDELSE_DATO_OPPRETTET("tiltakshendelse_dato_opprettet");

    public final String value;

    Sorteringsfelt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static Sorteringsfelt nameFromValue(String value) {
        for (Sorteringsfelt sorteringsfelt : values()) {
            if (sorteringsfelt.value.equals(value)) {
                return sorteringsfelt;
            }
        }
        throw new IllegalArgumentException("Ugyldig verdi for enum: " + value ); // TODO betre feilhandtering ved ugyldige values?
    }
}
