package no.nav.pto.veilarbportefolje.skjerming;

import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;

public class SkjermingData {
    private final Fnr fnr;
    private final boolean er_skjermet;
    private final Timestamp skjermet_fra;
    private final Timestamp skjermet_til;

    @java.beans.ConstructorProperties({"fnr", "er_skjermet", "skjermet_fra", "skjermet_til"})
    public SkjermingData(Fnr fnr, boolean er_skjermet, Timestamp skjermet_fra, Timestamp skjermet_til) {
        this.fnr = fnr;
        this.er_skjermet = er_skjermet;
        this.skjermet_fra = skjermet_fra;
        this.skjermet_til = skjermet_til;
    }

    public Fnr getFnr() {
        return this.fnr;
    }

    public boolean isEr_skjermet() {
        return this.er_skjermet;
    }

    public Timestamp getSkjermet_fra() {
        return this.skjermet_fra;
    }

    public Timestamp getSkjermet_til() {
        return this.skjermet_til;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SkjermingData)) return false;
        final SkjermingData other = (SkjermingData) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$fnr = this.getFnr();
        final Object other$fnr = other.getFnr();
        if (this$fnr == null ? other$fnr != null : !this$fnr.equals(other$fnr)) return false;
        if (this.isEr_skjermet() != other.isEr_skjermet()) return false;
        final Object this$skjermet_fra = this.getSkjermet_fra();
        final Object other$skjermet_fra = other.getSkjermet_fra();
        if (this$skjermet_fra == null ? other$skjermet_fra != null : !this$skjermet_fra.equals(other$skjermet_fra))
            return false;
        final Object this$skjermet_til = this.getSkjermet_til();
        final Object other$skjermet_til = other.getSkjermet_til();
        if (this$skjermet_til == null ? other$skjermet_til != null : !this$skjermet_til.equals(other$skjermet_til))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SkjermingData;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fnr = this.getFnr();
        result = result * PRIME + ($fnr == null ? 43 : $fnr.hashCode());
        result = result * PRIME + (this.isEr_skjermet() ? 79 : 97);
        final Object $skjermet_fra = this.getSkjermet_fra();
        result = result * PRIME + ($skjermet_fra == null ? 43 : $skjermet_fra.hashCode());
        final Object $skjermet_til = this.getSkjermet_til();
        result = result * PRIME + ($skjermet_til == null ? 43 : $skjermet_til.hashCode());
        return result;
    }

    public String toString() {
        return "SkjermingData(fnr=" + this.getFnr() + ", er_skjermet=" + this.isEr_skjermet() + ", skjermet_fra=" + this.getSkjermet_fra() + ", skjermet_til=" + this.getSkjermet_til() + ")";
    }
}

