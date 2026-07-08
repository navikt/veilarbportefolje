package no.nav.pto.veilarbportefolje.opensearch.domene;

import java.util.List;

public class Hits {
    HitsTotal total;
    List<Hit> hits;

    public Hits() {
    }

    public HitsTotal getTotal() {
        return this.total;
    }

    public List<Hit> getHits() {
        return this.hits;
    }

    public void setTotal(HitsTotal total) {
        this.total = total;
    }

    public void setHits(List<Hit> hits) {
        this.hits = hits;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Hits)) return false;
        final Hits other = (Hits) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$total = this.getTotal();
        final Object other$total = other.getTotal();
        if (this$total == null ? other$total != null : !this$total.equals(other$total)) return false;
        final Object this$hits = this.getHits();
        final Object other$hits = other.getHits();
        if (this$hits == null ? other$hits != null : !this$hits.equals(other$hits)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Hits;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $total = this.getTotal();
        result = result * PRIME + ($total == null ? 43 : $total.hashCode());
        final Object $hits = this.getHits();
        result = result * PRIME + ($hits == null ? 43 : $hits.hashCode());
        return result;
    }

    public String toString() {
        return "Hits(total=" + this.getTotal() + ", hits=" + this.getHits() + ")";
    }

    public static class HitsTotal {
        int value;

        public HitsTotal() {
        }

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof HitsTotal)) return false;
            final HitsTotal other = (HitsTotal) o;
            if (!other.canEqual((Object) this)) return false;
            if (this.getValue() != other.getValue()) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof HitsTotal;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getValue();
            return result;
        }

        public String toString() {
            return "Hits.HitsTotal(value=" + this.getValue() + ")";
        }
    }
}
