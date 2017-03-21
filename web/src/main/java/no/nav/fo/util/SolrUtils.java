package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Facet;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder, Comparator erNyComparator) {

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

    static Comparator<Bruker> brukerNavnComparator() {
        return (brukerA, brukerB) -> {

            Locale locale = new Locale("no", "NO");

            Collator collator = Collator.getInstance(locale);
            collator.setStrength(Collator.PRIMARY);

            String etternavnA = brukerA.getEtternavn();
            String etternavnB = brukerB.getEtternavn();

            String fornavnA = brukerA.getFornavn();
            String fornavnB = brukerB.getFornavn();

            if (collator.compare(etternavnA, etternavnB) < 0) {
                return -1;
            } else if (collator.compare(etternavnA, etternavnB) > 0) {
                return 1;
            } else {
                if (collator.compare(fornavnA, fornavnB) < 0) {
                    return -1;
                } else if (collator.compare(fornavnA, fornavnB) > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg) {
        List<String> statements = new ArrayList<>();

        if (filtervalg.nyeBrukere) {
            statements.add("-veileder_id:*");
        }

        if (filtervalg.inaktiveBrukere) {
            statements.add("(formidlingsgruppekode:ISERV AND veileder_id:*)");
        }

        if (filtervalg.harYtelsefilter()) {
            filtervalg.ytelser.forEach((ytelse) -> statements.add(format("ytelser:%s", ytelse.toString())));
        }

        if (!statements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(statements, " OR "));
        }
    }
}
