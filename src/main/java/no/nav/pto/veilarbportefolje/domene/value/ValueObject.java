package no.nav.pto.veilarbportefolje.domene.value;

import java.util.Objects;

abstract class ValueObject<V> {
    protected final V value;

    protected ValueObject(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueObject aktoerId = (ValueObject) o;
        return Objects.equals(value, aktoerId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
