package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;

@Data
@Accessors(chain = true)
public class OppfolgingsBruker {
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
    boolean har_delt_cv;
    boolean cv_eksistere;
    boolean egen_ansatt;
    boolean er_doed;
    String veileder_id;
    int fodselsdag_i_mnd;
    String fodselsdato;
    String kjonn;
    String ytelse;
    String utlopsdato;
    Integer dagputlopuke;
    Integer permutlopuke;
    Integer aapmaxtiduke;
    Integer aapunntakukerigjen;
    boolean oppfolging;
    boolean ny_for_veileder;
    boolean trenger_vurdering;
    String venterpasvarfrabruker;
    String venterpasvarfranav;
    String nyesteutlopteaktivitet;
    String aktivitet_start;
    String neste_aktivitet_start;
    String forrige_aktivitet_start;
    String manuell_bruker;
    String aktivitet_mote_startdato = getFarInTheFutureDate();
    String aktivitet_mote_utlopsdato = getFarInTheFutureDate();
    String aktivitet_stilling_utlopsdato = getFarInTheFutureDate();
    String aktivitet_egen_utlopsdato = getFarInTheFutureDate();
    String aktivitet_behandling_utlopsdato = getFarInTheFutureDate();
    String aktivitet_ijobb_utlopsdato = getFarInTheFutureDate();
    String aktivitet_sokeavtale_utlopsdato = getFarInTheFutureDate();
    String aktivitet_tiltak_utlopsdato = getFarInTheFutureDate();
    String aktivitet_utdanningaktivitet_utlopsdato = getFarInTheFutureDate();
    String aktivitet_gruppeaktivitet_utlopsdato = getFarInTheFutureDate();

    String alle_aktiviteter_mote_startdato = getFarInTheFutureDate();
    String alle_aktiviteter_mote_utlopsdato = getFarInTheFutureDate();
    String alle_aktiviteter_stilling_utlopsdato = getFarInTheFutureDate();
    String alle_aktiviteter_egen_utlopsdato = getFarInTheFutureDate();
    String alle_aktiviteter_behandling_utlopsdato = getFarInTheFutureDate();
    String alle_aktiviteter_ijobb_utlopsdato = getFarInTheFutureDate();
    String alle_aktiviteter_sokeavtale_utlopsdato = getFarInTheFutureDate();
    boolean arbeidsliste_aktiv;
    String arbeidsliste_sist_endret_av_veilederid;
    String arbeidsliste_endringstidspunkt;
    String arbeidsliste_frist;
    String arbeidsliste_kategori;
    String arbeidsliste_tittel_sortering;
    int arbeidsliste_tittel_lengde;
    String oppfolging_startdato;
    Set<String> alleAktiviteter = emptySet();
    Set<String> aktiviteter = emptySet();
    Set<String> tiltak = emptySet();
    boolean har_veileder_fra_enhet;
    String vedtak_status;
    String vedtak_status_endret;
    String ansvarlig_veileder_for_vedtak;
    boolean trenger_revurdering;
    boolean er_sykmeldt_med_arbeidsgiver;
    String brukers_situasjon;
    String profilering_resultat;
    String utdanning;
    String utdanning_bestatt;
    String utdanning_godkjent;
    Map<String, Endring> siste_endringer;
}
