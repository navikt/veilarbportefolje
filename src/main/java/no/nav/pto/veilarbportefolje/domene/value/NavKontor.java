package no.nav.pto.veilarbportefolje.domene.value;

public final class NavKontor extends ValueObject<String> {
    public NavKontor(String value) {
        super(value);
    }

    public static NavKontor of(String value) {
        return new NavKontor(value);
    }
}
