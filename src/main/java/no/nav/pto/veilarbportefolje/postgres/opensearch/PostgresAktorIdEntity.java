package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PostgresAktorIdEntity {
    private String aktoerId;

    private String brukersSituasjon;
    private String profileringResultat;
    private String utdanning;
    private String utdanningBestatt;
    private String utdanningGodkjent;

    private Boolean harDeltCv;
    private Boolean cvEksistere;

    private Boolean oppfolging;
    private Boolean nyForVeileder;
    private Boolean manuellBruker;
    private String oppfolgingStartdato;

    private String venterpasvarfrabruker;
    private String venterpasvarfranav;

    private String vedtak14AStatus;
    private String vedtak14AStatusEndret;
    private String ansvarligVeilederFor14AVedtak;

    private boolean arbeidslisteAktiv = false;
    private String arbeidslisteSistEndretAvVeilederid;
    private String arbeidslisteEndringstidspunkt;
    private String arbeidslisteFrist;
    private String arbeidslisteKategori;
    private String arbeidslisteTittelSortering;
    private int arbeidslisteTittelLengde;

    private String ytelse;
    private String ytelseUtlopsdato;
    private Integer dagputlopuke;
    private Integer permutlopuke;
    private Integer aapmaxtiduke;
    private Integer aapunntakukerigjen;
}
