package no.nav.fo.domene;


import org.apache.solr.common.SolrDocument;

import java.util.*;

public class Bruker {
    private String fnr;
    private String fornavn;
    private String etternavn;
    private List<String> sikkerhetstiltak;
    private String diskresjonskode;
    private boolean egenAnsatt;
    private String veilderId;

    public String getFnr() {
        return fnr;
    }

    public String getFornavn() {
        return fornavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public String getDiskresjonskode() { return diskresjonskode; }

    public boolean getEgenAnsatt() { return egenAnsatt; }

    public List<String> getSikkerhetstiltak() { return sikkerhetstiltak; }

    public String getVeilderId() { return veilderId;}

    public Bruker() {
        sikkerhetstiltak = new ArrayList<>();
    }

    public static Bruker of(SolrDocument document) {
        return new Bruker()
                .withFnr((String) document.get("fodselsnr"))
                .withFornavn((String) document.get("fornavn"))
                .withEtternavn((String) document.get("etternavn"))
                .withVeilederId((String)document.get("veileder_id"));
    }

    public Bruker withFnr(String fnr) {
        this.fnr = fnr;
        return this;
    }

    public Bruker withFornavn(String fornavn) {
        this.fornavn = fornavn;
        return this;
    }

    public Bruker withEtternavn(String etternavn) {
        this.etternavn = etternavn;
        return this;
    }

    public Bruker withDiskresjonskode(String diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
        return this;
    }

    public Bruker addSikkerhetstiltak(String sikkerhetstiltak) {
        this.sikkerhetstiltak.add(sikkerhetstiltak);
        return this;
    }

    public Bruker withVeilederId(String veilederId) {
        this.veilderId = veilederId;
        return this;
    }

    public Bruker erEgenAnsatt() {
        this.egenAnsatt = true;
        return this;
    }
}
