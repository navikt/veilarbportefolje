package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PostgresAktorIdEntity {
    String aktoerId;

    String brukers_situasjon;
    String profilering_resultat;
    String utdanning;
    String utdanning_bestatt;
    String utdanning_godkjent;

    Boolean har_delt_cv;
    Boolean cv_eksistere;

    Boolean oppfolging;
    Boolean ny_for_veileder;
    Boolean manuell_bruker;
    String oppfolging_startdato;

    String venterpasvarfrabruker;
    String venterpasvarfranav;

    String vedtak_status;
    String vedtak_status_endret;
    String ansvarlig_veileder_for_vedtak;

    boolean arbeidsliste_aktiv = false;
    String arbeidsliste_sist_endret_av_veilederid;
    String arbeidsliste_endringstidspunkt;
    String arbeidsliste_frist;
    String arbeidsliste_kategori;
    String arbeidsliste_tittel_sortering;
    int arbeidsliste_tittel_lengde;
}
