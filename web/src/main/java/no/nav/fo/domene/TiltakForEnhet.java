package no.nav.fo.domene;

public class TiltakForEnhet {
    private String enhetid;
    private String tiltakskode;

    public TiltakForEnhet(String enhetid, String tiltakskode) {
        this.enhetid = enhetid;
        this.tiltakskode = tiltakskode;
    }

    public String getEnhetid() {
        return enhetid;
    }

    public String getTiltakskode() {
        return tiltakskode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TiltakForEnhet that = (TiltakForEnhet) o;

        if (!getEnhetid().equals(that.getEnhetid())) return false;
        return getTiltakskode().equals(that.getTiltakskode());
    }

    @Override
    public int hashCode() {
        int result = getEnhetid().hashCode();
        result = 31 * result + getTiltakskode().hashCode();
        return result;
    }
}
