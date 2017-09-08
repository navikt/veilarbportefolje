package no.nav.fo.filmottak.tiltak;

public class EnhetTilFnr {
    public String enhetId;
    public String fnr;

    public EnhetTilFnr(String enhetId, String fnr) {
        this.enhetId = enhetId;
        this.fnr = fnr;
    }

    public EnhetTilFnr() {}

    public String getEnhetId() {
        return this.enhetId;
    }

    public String getFnr() {
        return this.fnr;
    }

    public void setEnhetId(String enhetId) {
        this.enhetId = enhetId;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

}
