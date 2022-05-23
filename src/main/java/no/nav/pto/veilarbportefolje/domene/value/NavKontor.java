package no.nav.pto.veilarbportefolje.domene.value;

public final class NavKontor extends ValueObject<String> {
    public NavKontor(String value) {
        super(value);
    }

    public static NavKontor of(String value) {
        return new NavKontor(value);
    }

    public static NavKontor navKontorOrNull(String value) {
        if(value == null){
            return null;
        }
        return new NavKontor(value);
    }
}
