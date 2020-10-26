package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak;

import lombok.Value;

@Value(staticConstructor = "of")
public class TiltakForEnhet {
    private String enhetid;
    private String tiltakskode;

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
