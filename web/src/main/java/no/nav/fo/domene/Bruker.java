package no.nav.fo.domene;


import org.apache.solr.common.SolrDocument;

import java.util.*;

import static java.util.Collections.singletonList;

public class Bruker {
    public final String fodselsnr;
    public final String fornavn;
    public final String etternavn;
    public final List<String> sikkerhetstiltak;
    public final String diskresjonskode;
    public final boolean sperretAnsatt;
    public final String veilderId;

    public Bruker(SolrDocument document) {
        this.fodselsnr = (String)document.get("fodselsnr");
        this.fornavn = (String)document.get("fornavn");
        this.etternavn = (String)document.get("etternavn");
        this.sikkerhetstiltak = singletonList((String) document.get("sikkerhetstiltak_type_kode"));
        this.diskresjonskode = (String)document.get("diskresjonskode");
        this.sperretAnsatt = Boolean.parseBoolean((String) document.get("sperret_ansatt"));
        this.veilderId = (String)document.get("veilder_id");
    }
}
