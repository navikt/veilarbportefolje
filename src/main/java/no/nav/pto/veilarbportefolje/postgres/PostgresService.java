package no.nav.pto.veilarbportefolje.postgres;

import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class PostgresService {
    private final VedtakstottePilotRequest vedtakstottePilotRequest;
    private final VeilarbVeilederClient veilarbVeilederClient;
    private final JdbcTemplate jdbcTemplate;

    public PostgresService(VedtakstottePilotRequest vedtakstottePilotRequest, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, VeilarbVeilederClient veilarbVeilederClient) {
        this.vedtakstottePilotRequest = vedtakstottePilotRequest;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public BrukereMedAntall hentBrukere(String enhetId, String veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(enhetId));
        boolean vedtaksPilot = erVedtakstottePilotPa(EnhetId.of(enhetId));

        PostgresQueryBuilder query = new PostgresQueryBuilder(jdbcTemplate, enhetId, vedtaksPilot);

        boolean kallesFraMinOversikt = StringUtils.isNotBlank(veilederIdent);
        if (kallesFraMinOversikt) {
            query.minOversiktFilter(veilederIdent);
        }
        if (filtervalg.harAktiveFilter()) {
            if (filtervalg.harFerdigFilter()) {
                filtervalg.ferdigfilterListe.forEach(
                        filter -> leggTilFerdigFilter(query, filter, veiledereMedTilgangTilEnhet, vedtaksPilot)
                );
            }
            leggTilManuelleFilter(query, filtervalg);
        }

        return query.search(fra, antall);
    }

    private void leggTilManuelleFilter(PostgresQueryBuilder query, Filtervalg filtervalg) {
        List<Integer> fodseldagIMndQuery = filtervalg.fodselsdagIMnd.stream().map(Integer::parseInt).collect(toList());
        query.leggTilFodselsdagFilter(fodseldagIMndQuery);

        query.leggTilListeFilter(filtervalg.arbeidslisteKategori, PostgresTable.BRUKER_VIEW.ARB_KATEGORI);
        query.leggTilListeFilter(filtervalg.veiledere, PostgresTable.BRUKER_VIEW.VEILEDERID);
        query.leggTilListeFilter(filtervalg.formidlingsgruppe, PostgresTable.BRUKER_VIEW.FORMIDLINGSGRUPPEKODE);
        query.leggTilListeFilter(filtervalg.innsatsgruppe, PostgresTable.BRUKER_VIEW.KVALIFISERINGSGRUPPEKODE); // TODO: er det litt rart at disse to går mot samme felt?
        query.leggTilListeFilter(filtervalg.servicegruppe, PostgresTable.BRUKER_VIEW.KVALIFISERINGSGRUPPEKODE); // TODO: er det litt rart at disse to går mot samme felt?
        query.leggTilListeFilter(filtervalg.hovedmal, PostgresTable.BRUKER_VIEW.HOVEDMAALKODE);
        query.leggTilListeFilter(filtervalg.rettighetsgruppe, PostgresTable.BRUKER_VIEW.RETTIGHETSGRUPPEKODE);
        query.leggTilListeFilter(filtervalg.registreringstype, PostgresTable.BRUKER_VIEW.BRUKERS_SITUASJON);
        query.leggTilListeFilter(filtervalg.utdanning, PostgresTable.BRUKER_VIEW.UTDANNING);
        query.leggTilListeFilter(filtervalg.utdanningBestatt, PostgresTable.BRUKER_VIEW.UTDANNING_BESTATT);
        query.leggTilListeFilter(filtervalg.utdanningGodkjent, PostgresTable.BRUKER_VIEW.UTDANNING_GODKJENT);

        /*
        byggManuellFilter(filtervalg.manuellBrukerStatus, queryBuilder, "manuell_bruker");
        byggManuellFilter(filtervalg.tiltakstyper, queryBuilder, "tiltak");
        byggManuellFilter(filtervalg.aktiviteterForenklet, queryBuilder, "aktiviteter");
        */
        if (filtervalg.harNavnEllerFnrQuery()) {
            query.navnOgFodselsnummerSok(filtervalg.getNavnEllerFnrQuery());
        }
        if (!filtervalg.alder.isEmpty()) {
            query.alderFilter(filtervalg.alder);
        }
        if (filtervalg.harKjonnfilter()) {
            query.kjonnfilter(filtervalg.kjonn);
        }

        if (filtervalg.harCvFilter()) {
            if(filtervalg.cvJobbprofil.equals(CVjobbprofil.HAR_DELT_CV)){
                query.harDeltCvFilter();
            } else if(filtervalg.cvJobbprofil.equals(CVjobbprofil.HAR_IKKE_DELT_CV)) {
                query.harIkkeDeltCvFilter();
            }
        }

        /*
        if (filtervalg.harYtelsefilter()) {
            BoolQueryBuilder subQuery = boolQuery();
            filtervalg.ytelse.underytelser.forEach(
                    ytelse -> queryBuilder.must(subQuery.should(matchQuery("ytelse", ytelse.name())))
            );
        }

        if (filtervalg.harAktivitetFilter()) {
            byggAktivitetFilterQuery(filtervalg, queryBuilder);
        }

        if (filtervalg.harUlesteEndringerFilter()) {
            byggUlestEndringsFilter(filtervalg.sisteEndringKategori, queryBuilder);
        }

        if (filtervalg.harSisteEndringFilter()) {
            byggSisteEndringFilter(filtervalg.sisteEndringKategori, queryBuilder);
        }
         */
    }


    static QueryBuilder leggTilFerdigFilter(PostgresQueryBuilder query, Brukerstatus brukerStatus, List<String> veiledereMedTilgangTilEnhet, boolean erVedtakstottePilotPa) {
        LocalDate localDate = LocalDate.now();
        switch (brukerStatus) {
            case UFORDELTE_BRUKERE:
                query.ufordeltBruker(veiledereMedTilgangTilEnhet);
                break;
            case NYE_BRUKERE_FOR_VEILEDER:
                query.nyForVeileder();
                break;
            case INAKTIVE_BRUKERE:
                query.ikkeServiceBehov();
                break;
            case VENTER_PA_SVAR_FRA_NAV:
                query.venterPaSvarFraNav();
                break;
            case VENTER_PA_SVAR_FRA_BRUKER:
                query.venterPaSvarFraBruker();
                break;
            case I_AVTALT_AKTIVITET:
                // existsQuery("aktiviteter");
                break;
            case IKKE_I_AVTALT_AKTIVITET:
                // boolQuery().mustNot(existsQuery("aktiviteter"));
                break;
            case UTLOPTE_AKTIVITETER:
                // existsQuery("nyesteutlopteaktivitet");
                break;
            case MIN_ARBEIDSLISTE:
                query.harArbeidsliste();
                break;
            case MOTER_IDAG:
                /* = rangeQuery("aktivitet_mote_startdato")
                        .gte(toIsoUTC(localDate.atStartOfDay()))
                        .lt(toIsoUTC(localDate.plusDays(1).atStartOfDay()));*/
                break;
            case ER_SYKMELDT_MED_ARBEIDSGIVER:
                query.erSykmeldtMedArbeidsgiver(erVedtakstottePilotPa);
                break;
            case TRENGER_VURDERING:
                query.trengerVurdering(erVedtakstottePilotPa);
                break;
            case UNDER_VURDERING:
                query.underVurdering(erVedtakstottePilotPa);
                break;
            case NYE_BRUKERE:                       // Ikke lengre bruk???
                break;
            case PERMITTERTE_ETTER_NIENDE_MARS:     // Ikke lengre bruk
                // byggPermittertFilter();
                break;
            case IKKE_PERMITTERTE_ETTER_NIENDE_MARS: // Ikke lengre bruk
                // byggIkkePermittertFilter();
                break;
            default:
                throw new IllegalStateException();

        }
        return null;
    }

    public List<Bruker> hentBrukereMedArbeidsliste(String veilederId, String enhetId) {
        return null;
    }

    public StatusTall hentStatusTallForVeileder(String veilederId, String enhetId) {
        boolean vedtakstottePilotErPa = erVedtakstottePilotPa(EnhetId.of(enhetId));
        return null;
    }

    public StatusTall hentStatusTallForEnhet(String enhetId) {
        return null;
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        return null;
    }

    private boolean erVedtakstottePilotPa(EnhetId enhetId) {
        return vedtakstottePilotRequest.erVedtakstottePilotPa(enhetId);
    }
}
