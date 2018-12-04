package no.nav.fo.veilarbportefolje.indeksering;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.Set;

import static no.nav.fo.veilarbportefolje.util.DateUtils.getSolrMaxAsIsoUtc;

@Data
@Builder
public class BrukerDTO {
    @NotEmpty
    String person_id;

    @NotEmpty
    String fnr;

    @NotEmpty
    String fornavn;

    @NotEmpty
    String etternavn;

    @NotEmpty
    String enhet_id;

    @NotEmpty
    String formidlingsgruppekode;

    String iserv_fra_dato;
    String kvalifiseringsgruppekode;
    String rettighetsgruppekode;
    String hovedmaalkode;
    String sikkerhetstiltak;
    String diskresjonskode;
    Boolean egen_ansatt;
    Boolean er_doed;
    String doed_fra_dato;
    String veileder_id;
    String fodselsdag_i_mnd;
    String fodselsdato;
    String kjonn;
    String ytelse;
    String utlopsdato;
    String utlopsdatofasett;
    Integer dagputlopuke;
    String dagputlopukefasett;
    Integer permutlopuke;
    String permutlopukefasett;
    Integer aapmaxtiduke;
    String aapmaxtidukefasett;
    Integer aapunntakukerigjen;
    String aapunntakukerigjenfasett;
    Boolean oppfolging;
    Boolean ny_for_veileder;
    Boolean ny_for_enhet;
    Boolean trenger_vurdering;
    String venterpasvarfrabruker;
    String venterpasvarfranav;
    String nyesteutlopteaktivitet;
    String aktivitet_start;
    String neste_aktivitet_start;
    String forrige_aktivitet_start;
    String manuell_bruker;

    @Builder.Default
    String aktivitet_mote_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_stilling_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_egen_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_behandling_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_ijobb_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_sokeavtale_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_tiltak_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_utdanningaktivitet_utlopsdato = getSolrMaxAsIsoUtc();

    @Builder.Default
    String aktivitet_gruppeaktivitet_utlopsdato = getSolrMaxAsIsoUtc();

    //arbeidsliste
    Boolean arbeidsliste_aktiv;
    String arbeidsliste_sist_endret_av_veilederid;
    String arbeidsliste_endringstidspunkt;
    String arbeidsliste_kommentar;
    String arbeidsliste_overskrift;
    String arbeidsliste_frist;

    Set<String> aktiviteter;
    Set<String> tiltak;
}