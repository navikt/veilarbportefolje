package no.nav.pto.veilarbportefolje.opensearch.domene;

public class Hit {
    String _index;
    String _id;
    PortefoljebrukerOpensearchModell _source;

    public Hit() {
    }

    public String get_index() {
        return this._index;
    }

    public String get_id() {
        return this._id;
    }

    public PortefoljebrukerOpensearchModell get_source() {
        return this._source;
    }

    public void set_index(String _index) {
        this._index = _index;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void set_source(PortefoljebrukerOpensearchModell _source) {
        this._source = _source;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Hit)) return false;
        final Hit other = (Hit) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$_index = this.get_index();
        final Object other$_index = other.get_index();
        if (this$_index == null ? other$_index != null : !this$_index.equals(other$_index)) return false;
        final Object this$_id = this.get_id();
        final Object other$_id = other.get_id();
        if (this$_id == null ? other$_id != null : !this$_id.equals(other$_id)) return false;
        final Object this$_source = this.get_source();
        final Object other$_source = other.get_source();
        if (this$_source == null ? other$_source != null : !this$_source.equals(other$_source)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Hit;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $_index = this.get_index();
        result = result * PRIME + ($_index == null ? 43 : $_index.hashCode());
        final Object $_id = this.get_id();
        result = result * PRIME + ($_id == null ? 43 : $_id.hashCode());
        final Object $_source = this.get_source();
        result = result * PRIME + ($_source == null ? 43 : $_source.hashCode());
        return result;
    }

    public String toString() {
        return "Hit(_index=" + this.get_index() + ", _id=" + this.get_id() + ", _source=" + this.get_source() + ")";
    }
}

