package no.nav.fo.domene;


import org.apache.solr.common.SolrDocument;

import java.util.*;

public class Bruker {
    public final String fodselsnr;
    public final String fornavn;
    public final String etternavn;
    public final String sikkerhetstiltak;
    public final String diskresjonskode;
    public final boolean sperretAnsatt;
    public final String veilderId;

    public Bruker(SolrDocument document) {
        this.fodselsnr = (String)document.get("fodselsnr");
        this.fornavn = (String)document.get("fornavn");
        this.etternavn = (String)document.get("etternavn");
        this.sikkerhetstiltak = (String) document.get("sikkerhetstiltak_type_kode");
        this.diskresjonskode = (String)document.get("diskresjonskode");
        this.sperretAnsatt = (boolean) document.get("sperret_ansatt");
        this.veilderId = (String)document.get("veilder_id");
    }
}
