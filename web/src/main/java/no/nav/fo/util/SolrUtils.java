package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Facet;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg) {
        if(!filtervalg.harAktiveFilter()) {
            return;
        }

        List<String> oversiktStatements = new ArrayList<>();
        List<String> filtrerBrukereStatements = new ArrayList<>();

        if(filtervalg.nyeBrukere && filtervalg.inaktiveBrukere) {
            oversiktStatements.add("(formidlingsgruppekode:ISERV AND veileder_id:*) OR (*:* AND -veileder_id:*)");
        } else if(filtervalg.nyeBrukere) {
            oversiktStatements.add("-veileder_id:*");
        } else if(filtervalg.inaktiveBrukere) {
            oversiktStatements.add("(formidlingsgruppekode:ISERV AND veileder_id:*)");
        }

        if(filtervalg.alder > 0  && filtervalg.alder <= 8) {
            filtrerBrukereStatements.add(leggTilAlderFilter(filtervalg));
        }

        if(filtervalg.kjonn != null && ("K".equals(filtervalg.kjonn) || "M".equals(filtervalg.kjonn))) {
            filtrerBrukereStatements.add("kjonn:" + filtervalg.kjonn);
        }

        if(filtervalg.fodselsdagIMnd > 0 && filtervalg.fodselsdagIMnd <= 31) {
            filtrerBrukereStatements.add("fodselsdag_i_mnd:" + filtervalg.fodselsdagIMnd);
        }

        if(!oversiktStatements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(oversiktStatements, " OR "));
        }

        if(!filtrerBrukereStatements.isEmpty()) {
            query.addFilterQuery(StringUtils.join(filtrerBrukereStatements, " AND "));
        }
    }

    static String leggTilAlderFilter(Filtervalg filtervalg) {
        String filter = "fodselsdato:";
        String NOW_PREFIX = "[NOW/DAY-"; // '/DAY' runder ned til dagen for å kunne bruke cache
        String POSTFIX = "+1DAY/DAY]"; // NOW+1DAY/DAY velger midnatt som kommer istedenfor midnatt som var, '/DAY' for å bruke cache

        // Pga. at man fortsatt er f.eks 19år når man er 19år og 364 dager så ser spørringene litt rare ut i forhold til ønsket filter
        switch(filtervalg.alder) {
            case 1:
                return filter += NOW_PREFIX + "20YEARS+1DAY TO NOW" + POSTFIX; // 19 og under
            case 2:
                return filter += NOW_PREFIX + "25YEARS+1DAY TO NOW-20YEARS" + POSTFIX; // 20-24
            case 3:
                return filter += NOW_PREFIX + "30YEARS+1DAY TO NOW-25YEARS" + POSTFIX; // 25-29
            case 4:
                return filter += NOW_PREFIX + "40YEARS+1DAY TO NOW-30YEARS" + POSTFIX; // 30-39
            case 5:
                return filter += NOW_PREFIX + "50YEARS+1DAY TO NOW-40YEARS" + POSTFIX; // 40-49
            case 6:
                return filter += NOW_PREFIX + "60YEARS+1DAY TO NOW-50YEARS" + POSTFIX; // 50-59
            case 7:
                return filter += NOW_PREFIX + "67YEARS+1DAY TO NOW-60YEARS" + POSTFIX; // 60-66
            default:
                return filter += NOW_PREFIX + "71YEARS+1DAY TO NOW-67YEARS" + POSTFIX; // 67-70
        }
    }
}
