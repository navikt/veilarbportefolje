package no.nav.fo.veilarbportefolje.indeksering.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;
import static no.nav.fo.veilarbportefolje.util.DateUtils.getSolrMaxAsIsoUtc;

@Data
@Accessors(chain = true)
public class OppfolgingsBruker {
    String person_id;
    String fnr;
    String aktoer_id;
    String fornavn;
    String etternavn;
    String fullt_navn;
    String enhet_id;
    String formidlingsgruppekode;
    String iserv_fra_dato;
    String kvalifiseringsgruppekode;
    String rettighetsgruppekode;
    String hovedmaalkode;
    String sikkerhetstiltak;
    String diskresjonskode;
    boolean egen_ansatt;
    boolean er_doed;
    String doed_fra_dato;
    String veileder_id;
    int fodselsdag_i_mnd;
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
    boolean oppfolging;
    boolean ny_for_veileder;
    boolean ny_for_enhet;
    boolean trenger_vurdering;
    String venterpasvarfrabruker;
    String venterpasvarfranav;
    String nyesteutlopteaktivitet;
    String aktivitet_start;
    String neste_aktivitet_start;
    String forrige_aktivitet_start;
    String manuell_bruker;
    String aktivitet_mote_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_stilling_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_egen_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_behandling_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_ijobb_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_sokeavtale_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_tiltak_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_utdanningaktivitet_utlopsdato = getSolrMaxAsIsoUtc();
    String aktivitet_gruppeaktivitet_utlopsdato = getSolrMaxAsIsoUtc();
    boolean arbeidsliste_aktiv;
    String arbeidsliste_sist_endret_av_veilederid;
    String arbeidsliste_endringstidspunkt;
    String arbeidsliste_kommentar;
    String arbeidsliste_overskrift;
    String arbeidsliste_frist;
    Set<String> aktiviteter = emptySet();
    Set<String> tiltak = emptySet();
    boolean har_veileder_fra_enhet;
}
