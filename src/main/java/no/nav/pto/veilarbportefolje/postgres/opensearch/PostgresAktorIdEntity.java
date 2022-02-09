package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PostgresAktorIdEntity {
    String aktoerId;

    String brukersSituasjon;
    String profileringResultat;
    String utdanning;
    String utdanningBestatt;
    String utdanningGodkjent;

    Boolean harDeltCv;
    Boolean cvEksistere;

    Boolean oppfolging;
    Boolean nyForVeileder;
    Boolean manuellBruker;
    String oppfolgingStartdato;

    String venterpasvarfrabruker;
    String venterpasvarfranav;

    String vedtak14AStatus;
    String vedtak14AStatusEndret;
    String ansvarligVeilederFor14AVedtak;

    boolean arbeidslisteAktiv = false;
    String arbeidslisteSistEndretAvVeilederid;
    String arbeidslisteEndringstidspunkt;
    String arbeidslisteFrist;
    String arbeidslisteKategori;
    String arbeidslisteTittelSortering;
    int arbeidslisteTittelLengde;
}
