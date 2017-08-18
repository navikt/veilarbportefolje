package no.nav.fo.util;

import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetTyper;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class SolrUtils {

    private static Locale locale = new Locale("no", "NO");
    private static Collator collator = Collator.getInstance(locale);
    static {
        collator.setStrength(Collator.PRIMARY);
    }

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
        solrQuery.addSort("person_id", SolrQuery.ORDER.asc);
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

    private static <S extends Comparable<S>> List<Bruker> sorterBrukerePaaFelt(List<Bruker> brukere, String sortOrder, Function<Bruker, S> sortField) {
        boolean ascending = "ascending".equals(sortOrder);

        Comparator<S> allowNullComparator = (o1, o2) -> {
            if(o1 == null && o2 == null) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;

            if (o1 instanceof String && o2 instanceof String) {
                return collator.compare(o1, o2);
            }
            return o1.compareTo(o2);
        };

        Comparator<Bruker> fieldComparator = Comparator.comparing(sortField, allowNullComparator);
        if (!ascending) {
            fieldComparator = fieldComparator.reversed();
        }

        Comparator<Bruker> comparator = brukerErNyComparator().thenComparing(fieldComparator);

        brukere.sort(comparator);
        return brukere;
    }

    public static List<Bruker> sortBrukere(List<Bruker> brukere, String sortOrder, String sortField) {
        if ("etternavn".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getEtternavn);
        } else if ("fodselsnummer".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getFnr);
        } else if ("utlopsdato".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getUtlopsdato);
        } else if ("aapMaxtid".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getAapMaxtid);
        } else if ("arbeidsliste_frist".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getArbeidslisteFrist);
        } else if ("VENTER_PA_SVAR_FRA_NAV".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getVenterPaSvarFraNAV);
        } else if ("VENTER_PA_SVAR_FRA_BRUKER".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getVenterPaSvarFraBruker);
        } else if ("UTLOPTE_AKTIVITETER".equals(sortField)) {
            return sorterBrukerePaaFelt(brukere, sortOrder, Bruker::getNyesteUtlopteAktivitet);
        }
        brukere.sort(brukerErNyComparator());
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

    private static <S, T> Comparator<S> norskComparator(final Function<S, T> keyExtractor) {
        return (S s1, S s2) -> collator.compare(keyExtractor.apply(s1), keyExtractor.apply(s2));
    }

    static Comparator<Bruker> brukerNavnComparator() {
        return norskComparator(Bruker::getEtternavn).thenComparing(norskComparator(Bruker::getFornavn));
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
        final List<String> filtrerBrukereStatements = new ArrayList<>();

        if (filtervalg.brukerstatus == Brukerstatus.NYE_BRUKERE) {
            oversiktStatements.add("-veileder_id:*");
        } else if (filtervalg.brukerstatus == Brukerstatus.INAKTIVE_BRUKERE) {
            oversiktStatements.add("(formidlingsgruppekode:ISERV)");
        } else if (filtervalg.brukerstatus == Brukerstatus.VENTER_PA_SVAR_FRA_NAV) {
            oversiktStatements.add("(venterpasvarfranav:*)");
        } else if (filtervalg.brukerstatus == Brukerstatus.VENTER_PA_SVAR_FRA_BRUKER) {
            oversiktStatements.add("(venterpasvarfrabruker:*)");
        } else if (filtervalg.brukerstatus == Brukerstatus.I_AVTALT_AKTIVITET ) {
            oversiktStatements.add("(iavtaltaktivitet:true)");
        } else if(filtervalg.brukerstatus == Brukerstatus.IKKE_I_AVTALT_AKTIVITET) {
            oversiktStatements.add("(-iavtaltaktivitet:true)");
        } else if(filtervalg.brukerstatus == Brukerstatus.UTLOPTE_AKTIVITETER) {
            oversiktStatements.add("(nyesteutlopteaktivitet:*)");
        } else if(filtervalg.brukerstatus == Brukerstatus.MIN_ARBEIDSLISTE) {
            oversiktStatements.add("(arbeidsliste_aktiv:*)");
        }


        filtrerBrukereStatements.add(orStatement(filtervalg.alder, SolrUtils::alderFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.kjonn, SolrUtils::kjonnFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.fodselsdagIMnd, SolrUtils::fodselsdagIMndFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.innsatsgruppe, SolrUtils::innsatsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.formidlingsgruppe, SolrUtils::formidlingsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.servicegruppe, SolrUtils::servicegruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.rettighetsgruppe, SolrUtils::rettighetsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.veiledere, SolrUtils::veilederFilter));

        for (Map.Entry<String, AktivitetFiltervalg> entry: filtervalg.aktiviteter.entrySet()) {
            if(entry.getValue() == AktivitetFiltervalg.JA) {
                filtrerBrukereStatements.add(entry.getKey().toLowerCase() + ":*");
            } else if (entry.getValue() == AktivitetFiltervalg.NEI) {
                filtrerBrukereStatements.add("-" + entry.getKey().toLowerCase() + ":*");
            }
        }


        if (filtervalg.harYtelsefilter()) {
            filtrerBrukereStatements.add(orStatement(filtervalg.ytelse.underytelser, SolrUtils::ytelseFilter));
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

    private static String ytelseFilter(YtelseMapping ytelse) {
        return "ytelse:" + ytelse;
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

    static String rettighetsgruppeFilter(Rettighetsgruppe rettighetsgruppe) {
        return "rettighetsgruppekode:" + rettighetsgruppe;
    }

    static String veilederFilter(String veileder) {
        return "veileder_id:" + veileder;
    }
}
