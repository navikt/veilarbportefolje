package no.nav.fo.util;

import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class SolrUtils {

    public static FacetResults mapFacetResults(FacetField facetField) {
        return new FacetResults()
            .setFacetResults(
                facetField.getValues().stream().map(
                    value -> new Facet()
                        .setValue(value.getName())
                        .setCount(value.getCount())
                ).collect(toList())
            );
    }

    public static SolrQuery buildSolrFacetQuery(String query, String facetField) {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(facetField);
        return solrQuery;
    }

    public static SolrQuery buildSolrQuery(String queryString) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery(queryString);
        return solrQuery;
    }


    public static SolrQuery buildSolrQuery(String queryString, Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery(queryString);
        leggTilFiltervalg(solrQuery, filtervalg);
        return solrQuery;
    }

    public static SolrQuery buildSolrQuery(Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        leggTilFiltervalg(solrQuery, filtervalg);
        return solrQuery;
    }

    public static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !Boolean.parseBoolean(isMasterString);
    }

    public static void checkSolrResponseCode(int statusCode) {
        if (statusCode != 0) {
            throw new SolrUpdateResponseCodeException(format("Solr returnerte med statuskode %s", statusCode));
        }
    }

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder, Comparator<Bruker> erNyComparator) {

        Comparator<Bruker> comparator = null;

        if (erNyComparator != null) {
            comparator = erNyComparator;

            if (sortOrder.equals("ascending") || sortOrder.equals("descending")) {
                comparator = comparator.thenComparing(setComparatorSortOrder(brukerNavnComparator(), sortOrder));
            }
        } else {
            if (sortOrder.equals("ascending") || sortOrder.equals("descending")) {
                comparator = setComparatorSortOrder(brukerNavnComparator(), sortOrder);
            }
        }

        if (comparator != null) {
            brukere.sort(comparator);
        }

        return brukere;
    }

    static Comparator<Bruker> setComparatorSortOrder(Comparator<Bruker> comparator, String sortOrder) {
        return sortOrder.equals("descending") ? comparator.reversed() : comparator;
    }

    public static Comparator<Bruker> brukerErNyComparator() {
        return (brukerA, brukerB) -> {

            boolean brukerAErNy = brukerA.getVeilederId() == null;
            boolean brukerBErNy = brukerB.getVeilederId() == null;

            if (brukerAErNy && !brukerBErNy) {
                return -1;
            } else if (!brukerAErNy && brukerBErNy) {
                return 1;
            } else {
                return 0;
            }
        };
    }

    private static <S> Comparator<S> norskComparator(final Function<S, String> keyExtractor) {
        Locale locale = new Locale("no", "NO");
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);


                return (S s1, S s2) ->collator.compare(keyExtractor.apply(s1), keyExtractor.apply(s2) );
                }
            static Comparator<Bruker> brukerNavnComparator() {
                    return norskComparator(Bruker::getEtternavn).thenComparing(norskComparator(Bruker::getFornavn));
    }

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg) {
        if (!filtervalg.harAktiveFilter()) {
            return;
        }

        List<String> oversiktStatements = new ArrayList<>();
        List<String> filtrerBrukereStatements = new ArrayList<>();

        if (filtervalg.nyeBrukere && filtervalg.inaktiveBrukere) {
            oversiktStatements.add("(formidlingsgruppekode:ISERV AND veileder_id:*) OR (*:* AND -veileder_id:*)");
        } else if (filtervalg.nyeBrukere) {
            oversiktStatements.add("-veileder_id:*");
        } else if (filtervalg.inaktiveBrukere) {
            oversiktStatements.add("(formidlingsgruppekode:ISERV AND veileder_id:*)");
        }

        if (filtervalg.alder != null && !filtervalg.alder.isEmpty()) {
            filtrerBrukereStatements.add(alderFilter(filtervalg.alder));
        }

        if (filtervalg.harYtelsefilter()) {
            filtrerBrukereStatements.add(format("ytelse:%s", filtervalg.ytelse));
        }

        if (filtervalg.kjonn != null && (filtervalg.kjonn == 0 || filtervalg.kjonn == 1)) {
            filtrerBrukereStatements.add("kjonn:" + FiltervalgMappers.kjonn[filtervalg.kjonn]);
        }

        if (filtervalg.fodselsdagIMnd != null && !filtervalg.fodselsdagIMnd.isEmpty()) {
            List<String> params = filtervalg.fodselsdagIMnd.stream().map(SolrUtils::fodselsdagIMndFilter).collect(toList());
            filtrerBrukereStatements.add(StringUtils.join(params, " OR "));
        }

        if (filtervalg.innsatsgruppe != null && !filtervalg.innsatsgruppe.isEmpty()) {
            List<String> params = filtervalg.innsatsgruppe.stream().map(SolrUtils::innsatsgruppeFilter).collect(toList());
            filtrerBrukereStatements.add(StringUtils.join(params, " OR "));
        }

        if (filtervalg.formidlingsgruppe != null && !filtervalg.formidlingsgruppe.isEmpty()) {
            List<String> params = filtervalg.formidlingsgruppe.stream().map(SolrUtils::formidlingsgruppeFilter).collect(toList());
            filtrerBrukereStatements.add(StringUtils.join(params, " OR "));
        }

        if (!oversiktStatements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(oversiktStatements, " OR "));
        }

        if (!filtrerBrukereStatements.isEmpty()) {
            query.addFilterQuery(filtrerBrukereStatements
                .stream().map(statement -> "(" + statement + ")").collect(Collectors.joining(" AND ")));
        }
    }

    static String alderFilter(List<Integer> index) {
        String filter = "fodselsdato:";

        List<String> params = index.stream().map((aldersRangeIndex) ->
            FiltervalgMappers.alder[aldersRangeIndex]).collect(Collectors.toList());
        return filter + StringUtils.join(params, " OR " + filter);
    }

    private static String fodselsdagIMndFilter(int index) {
        return "fodselsdag_i_mnd:" + FiltervalgMappers.fodselsdagIMnd[index];
    }

    static String innsatsgruppeFilter(int index) {
        return "kvalifiseringsgruppekode:" + FiltervalgMappers.innsatsgruppe[index];
    }

    static String formidlingsgruppeFilter(int index) {
        return "formidlingsgruppekode:" + FiltervalgMappers.formidlingsgruppe[index];
    }
}
