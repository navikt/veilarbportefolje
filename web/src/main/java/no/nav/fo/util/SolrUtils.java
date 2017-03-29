package no.nav.fo.util;

import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import javax.xml.ws.Service;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
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

    public static SolrQuery buildSolrQuery(String queryString, Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery(queryString);
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

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder, String sortField, Comparator<Bruker> erNyComparator) {
        // TODO Se på om denne kan skrives om litt
        Comparator<Bruker> comparator = null;
        Comparator<Bruker> fieldComparator = "etternavn".equals(sortField) ? brukerNavnComparator() : brukerFodelsdatoComparator();
        boolean hasSortOrder = "ascending".equals(sortOrder) || "descending".equals(sortOrder);

        if (erNyComparator != null) {
            comparator = erNyComparator;

            if (hasSortOrder) {
                comparator = comparator.thenComparing(setComparatorSortOrder(fieldComparator, sortOrder));
            }
        } else {
            if (hasSortOrder) {
                comparator = setComparatorSortOrder(fieldComparator, sortOrder);
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

        return (S s1, S s2) -> collator.compare(keyExtractor.apply(s1), keyExtractor.apply(s2));
    }

    static Comparator<Bruker> brukerNavnComparator() {
        return norskComparator(Bruker::getEtternavn).thenComparing(norskComparator(Bruker::getFornavn));
    }

    private static Comparator<Bruker> brukerFodelsdatoComparator() {
        return norskComparator(Bruker::getFnr);
    }

    public static <T> String orStatement(List<T> filter, Function<T, String> mapper) {
        if (filter == null || filter.isEmpty()) {
            return "";
        }
        return filter.stream().map(mapper).collect(joining(" OR "));
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

        filtrerBrukereStatements.add(orStatement(filtervalg.alder, SolrUtils::alderFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.kjonn, SolrUtils::kjonnFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.fodselsdagIMnd, SolrUtils::fodselsdagIMndFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.innsatsgruppe, SolrUtils::innsatsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.formidlingsgruppe, SolrUtils::formidlingsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.servicegruppe, SolrUtils::servicegruppeFilter));


        if (filtervalg.harYtelsefilter()) {
            filtrerBrukereStatements.add(format("ytelse:%s", filtervalg.ytelse));
        }

        if (!oversiktStatements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(oversiktStatements, " OR "));
        }

        if (!filtrerBrukereStatements.isEmpty()) {
            query.addFilterQuery(filtrerBrukereStatements
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .map(statement -> "(" + statement + ")")
                    .collect(Collectors.joining(" AND ")));
        }
    }

    static String kjonnFilter(Kjonn kjonn) {
        return "kjonn:" + kjonn.toString();
    }

    static String alderFilter(String alder) {
        return "fodselsdato:" + FiltervalgMappers.alder.get(alder);
    }

    static String fodselsdagIMndFilter(String fodselDato) {
        return "fodselsdag_i_mnd:" + fodselDato;
    }

    static String innsatsgruppeFilter(Innsatsgruppe innsatsgruppe) {
        return "kvalifiseringsgruppekode:" + innsatsgruppe;
    }

    static String formidlingsgruppeFilter(Formidlingsgruppe formidlingsgruppe) {
        return "formidlingsgruppekode:" + formidlingsgruppe;
    }

    static String servicegruppeFilter(Servicegruppe servicegruppe) {
        return "kvalifiseringsgruppekode:" + servicegruppe;
    }
}
