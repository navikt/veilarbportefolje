package no.nav.fo.domene;


public class Bruker {
    private String fnr;
    private String fornavn;
    private String etternavn;

    public String getFnr() {
        return fnr;
    }

    public String getFornavn() {
        return fornavn;
    }

    public String getEtternavn() {
        return etternavn;
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
}
