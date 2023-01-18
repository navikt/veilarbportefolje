package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

@Slf4j
public class TiltakkodeverkMapper {

    private static final Map<String, String> tiltakskodeTiltaksnavnMap = new HashMap<>(
            Map.ofEntries(
                    entry("LONNTILS", "Lønnstilskudd"),
                    entry("MENTOR", "Mentor"),
                    entry("MIDLONTIL", "Midlertidig lønnstilskudd"),
                    entry("NETTAMO", "Nettbasert arbeidsmarkedsopplæring (AMO)"),
                    entry("NETTKURS", "Nettkurs"),
                    entry("OPPLT2AAR", "2-årig opplæringstiltak"),
                    entry("PRAKSORD", "Arbeidspraksis i ordinær virksomhet"),
                    entry("PV", "Produksjonsverksted (PV)"),
                    entry("REAKTUFOR", "Lønnstilskudd - reaktivisering av uførepensjonister"),
                    entry("REFINO", "Resultatbasert finansiering av oppfølging"),
                    entry("SPA", "Spa prosjekter"),
                    entry("STATLAERL", "Lærlinger i statlige etater"),
                    entry("SUPPEMP", "Supported Employment"),
                    entry("ETAB", "Egenetablering"),
                    entry("FLEKSJOBB", "Fleksibel jobb - lønnstilskudd av lengre varighet"),
                    entry("FORSAMOENK", "Forsøk AMO enkeltplass"),
                    entry("FORSAMOGRU", "Forsøk AMO gruppe"),
                    entry("FORSFAGENK", "Forsøk fag- og yrkesopplæring enkeltplass"),
                    entry("FORSFAGGRU", "Forsøk fag- og yrkesopplæring gruppe"),
                    entry("FORSHOYUTD", "Forsøk høyere utdanning"),
                    entry("FUNKSJASS", "Funksjonsassistanse"),
                    entry("GRUNNSKOLE", "Grunnskole"),
                    entry("GRUPPEAMO", "Gruppe AMO"),
                    entry("HOYEREUTD", "Høyere utdanning"),
                    entry("HOYSKOLE", "Høyskole/Universitet"),
                    entry("INDJOBSTOT", "Individuell jobbstøtte (IPS)"),
                    entry("INDOPPFAG", "Oppfølging"),
                    entry("INDOPPFOLG", "Individuelt oppfølgingstiltak"),
                    entry("INDOPPFSP", "Oppfølging - sykmeldt arbeidstaker"),
                    entry("INDOPPRF", "Resultatbasert finansiering av formidlingsbistand"),
                    entry("INKLUTILS", "Inkluderingstilskudd"),
                    entry("INST_S", "Nye plasser institusjonelle tiltak"),
                    entry("IPSUNG", "Individuell karrierestøtte (IPS Ung)"),
                    entry("ITGRTILS", "Integreringstilskudd"),
                    entry("JOBBBONUS", "Jobbklubb med bonusordning"),
                    entry("JOBBFOKUS", "Jobbfokus/Utvidet formidlingsbistand"),
                    entry("JOBBK", "Jobbklubb"),
                    entry("JOBBKLUBB", "Intern jobbklubb"),
                    entry("JOBBSKAP", "Jobbskapingsprosjekter"),
                    entry("KAT", "Formidlingstjenester"),
                    entry("KURS", "Andre kurs"),
                    entry("LONNTIL", "Tidsbegrenset lønnstilskudd"),
                    entry("LONNTILAAP", "Arbeidsavklaringspenger som lønnstilskudd"),
                    entry("ABIST", "Arbeid med Bistand (AB)"),
                    entry("ABOPPF", "Arbeid med bistandpublic static final String A oppfølging"),
                    entry("ABTBOPPF", "Arbeid med bistand B"),
                    entry("ABUOPPF", "Arbeid med bistand A utvidet oppfølging"),
                    entry("AMBF1", "AMB Avklaring (fase 1)"),
                    entry("AMBF2", "Kvalifisering i arbeidsmarkedsbedrift"),
                    entry("AMBF3", "Tilrettelagt arbeid i arbeidsmarkedsbedrift"),
                    entry("AMO", "Arbeidsmarkedsopplæring (AMO)"),
                    entry("AMOB", "Arbeidsmarkedsopplæring (AMO) i bedrift"),
                    entry("AMOE", "Arbeidsmarkedsopplæring (AMO) enkeltplass"),
                    entry("AMOY", "Arbeidsmarkedsopplæring (AMO) yrkeshemmede"),
                    entry("ANNUTDANN", "Annen utdanning"),
                    entry("ARBFORB", "Arbeidsforberedende trening (AFT)"),
                    entry("ARBRDAGSM", "Arbeidsrettet rehabilitering (dag) - sykmeldt arbeidstaker"),
                    entry("ARBRRDOGN", "Arbeidsrettet rehabilitering (døgn)"),
                    entry("ARBRRHBAG", "Arbeidsrettet rehabilitering"),
                    entry("ARBRRHBSM", "Arbeidsrettet rehabilitering - sykmeldt arbeidstaker"),
                    entry("ARBRRHDAG", "Arbeidsrettet rehabilitering (dag)"),
                    entry("ARBTREN", "Arbeidstrening"),
                    entry("ASV", "Arbeidssamvirke (ASV)"),
                    entry("ATG", "Arbeidstreningsgrupper"),
                    entry("AVKLARAG", "Avklaring"),
                    entry("AVKLARKV", "Avklaring av kortere varighet"),
                    entry("AVKLARSP", "Avklaring - sykmeldt arbeidstaker"),
                    entry("AVKLARSV", "Avklaring i skjermet virksomhet"),
                    entry("AVKLARUS", "Avklaring"),
                    entry("BIA", "Bedriftsintern attføring"),
                    entry("BREVKURS", "Brevkurs"),
                    entry("DIGIOPPARB", "Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)"),
                    entry("DIVTILT", "Diverse tiltak"),
                    entry("EKSPEBIST", "Ekspertbistand"),
                    entry("ENKELAMO", "Enkeltplass AMO"),
                    entry("ENKFAGYRKE", "Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning"),
                    entry("SYSSLANG", "Sysselsettingstiltak for langtidsledige"),
                    entry("SYSSOFF", "Sysselsettingstiltak i offentlig sektor for yrkeshemmede"),
                    entry("TIDSUBLONN", "Tidsubestemt lønnstilskudd"),
                    entry("TILPERBED", "Tilretteleggingstilskudd for arbeidssøker"),
                    entry("TILRETTEL", "Tilrettelegging for arbeidstaker"),
                    entry("TILRTILSK", "Forebyggings- og tilretteleggingstilskudd IA virksomheter og BHT-honorar"),
                    entry("TILSJOBB", "Tilskudd til sommerjobb"),
                    entry("UFØREPENLØ", "Uførepensjon som lønnstilskudd"),
                    entry("BIO", "Bedriftsintern opplæring (BIO)"),
                    entry("LONNTILL", "Lønnstilskudd av lengre varighet"),
                    entry("PRAKSKJERM", "Arbeidspraksis i skjermet virksomhet"),
                    entry("GRUFAGYRKE", "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning"),
                    entry("ARBDOGNSM", "Arbeidsrettet rehabilitering (døgn) - sykmeldt arbeidstaker"),
                    entry("UTBHLETTPS", "Utredning/behandling lettere psykiske lidelser"),
                    entry("UTBHPSLD", "Utredning/behandling lettere psykiske og sammensatte lidelser"),
                    entry("UTBHSAMLI", "Utredning/behandling sammensatte lidelser"),
                    entry("UTDPERMVIK", "Utdanningspermisjoner"),
                    entry("UTDYRK", "Utdanning"),
                    entry("UTVAOONAV", "Utvidet oppfølging i NAV"),
                    entry("UTVOPPFOPL", "Utvidet oppfølging i opplæring"),
                    entry("VALS", "Formidlingstjenester - Ventelønn"),
                    entry("VARLONTIL", "Varig lønnstilskudd"),
                    entry("VASV", "Varig tilrettelagt arbeid i skjermet virksomhet"),
                    entry("VATIAROR", "Varig tilrettelagt arbeid i ordinær virksomhet"),
                    entry("VIDRSKOLE", "Videregående skole"),
                    entry("VIKARBLED", "Utdanningsvikariater"),
                    entry("VV", "Varig vernet arbeid (VVA)"),
                    entry("YHEMMOFF", "Sysselsettingstiltak for yrkeshemmede")
            )
    );

    public static String mapTilTiltaknavn(String tiltakkode) {
        if (tiltakskodeTiltaksnavnMap.containsKey(tiltakkode)) {
            return tiltakskodeTiltaksnavnMap.get(tiltakkode);
        } else {
            log.warn("Klarte ikke å mappe tiltakkode: " + tiltakkode + " til korresponderende tiltaknavn. Dette betyr at vår interne mapping må oppdateres med ny tiltakskode/tiltaksnavn entry.");
            return ""; //TODO: Hva skal vi returnere hvis det ikke finnes? (Det vises i frontend) - loggmeldingen må også tilpasses basert på hva vi bestemmer oss for (si noe om hvordan man fikser opp i inkonsistens i data som det medfører)
        }
    }
}
