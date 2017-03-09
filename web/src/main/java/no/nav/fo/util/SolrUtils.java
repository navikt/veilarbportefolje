package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Facet;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.service.SolrUpdateResponseCodeException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.*;

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
        return new SolrQuery(queryString);
    }

    public static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !Boolean.parseBoolean(isMasterString);
    }

    public static void checkSolrResponseCode(int statusCode) {
        if (statusCode != 0) {
            throw new SolrUpdateResponseCodeException(String.format("Solr returnerte med statuskode %s", statusCode));
        }
    }

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder, Comparator erNyComparator) {

        Comparator<Bruker> comparator = null;

        if(erNyComparator != null) {
            comparator = erNyComparator;

            if(sortOrder.equals("ascending") || sortOrder.equals("descending")) {
                comparator = comparator.thenComparing(setComparatorSortOrder(brukerNavnComparator(), sortOrder));
            }
        }
        else {
            if(sortOrder.equals("ascending") || sortOrder.equals("descending")) {
                comparator = setComparatorSortOrder(brukerNavnComparator(), sortOrder);
            }
        }

        if(comparator != null) {
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

            if(brukerAErNy && !brukerBErNy) {
                return -1;
            }
            else if(!brukerAErNy && brukerBErNy) {
                return 1;
            }
            else {
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

            if(collator.compare(etternavnA, etternavnB) < 0) {
                return -1;
            }
            else if(collator.compare(etternavnA, etternavnB) > 0) {
                return 1;
            }
            else {
                if(collator.compare(fornavnA, fornavnB) < 0) {
                    return -1;
                }
                else if(collator.compare(fornavnA, fornavnB) > 0) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        };
    }


}
