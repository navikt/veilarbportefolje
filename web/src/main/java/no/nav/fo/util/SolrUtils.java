package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Facet;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public static SolrQuery buildSolrQuery(String enhetId, Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery("enhet_id:" + enhetId);
        leggTilFiltervalg(solrQuery, filtervalg);
        return solrQuery;
    }

    public static Map<String, Object> nyesteBruker(List<Map<String, Object>> brukere) {
        return brukere.stream().max(Comparator.comparing(r -> new DateTime(r.get("tidsstempel")).getMillis())).get();
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

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder) {

        Comparator<Bruker> brukerComparator = brukerComparator();

        brukerComparator = setComparatorSortOrder(brukerComparator, sortOrder);

        brukere.sort(brukerComparator);

        return brukere;
    }

    static Comparator<Bruker> setComparatorSortOrder(Comparator<Bruker> comparator, String sortOrder) {
        return sortOrder.equals("descending") ? comparator.reversed() : comparator;
    }

    static Comparator<Bruker> brukerComparator() {
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

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg) {
        List<String> statements = new ArrayList<>();

        if(filtervalg.nyeBrukere) {
            statements.add("-veileder_id:*");
        }

        if(filtervalg.inaktiveBrukere) {
            statements.add("(formidlingsgruppekode:ISERV AND veileder_id:*)");
        }

        if(!statements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(statements, " OR "));
        }
    }
}
