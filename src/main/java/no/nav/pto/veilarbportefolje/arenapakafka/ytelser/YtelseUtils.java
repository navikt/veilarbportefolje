package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

public class YtelseUtils {
    public static Integer konverterDagerTilUker(String antallDagerFraDB) {
        Integer antallDager = parseInteger(antallDagerFraDB);
        return konverterDagerTilUker(antallDager);
    }

    /*
    NB: 1 uke i arena er basert på antall arbeidsdager i en gitt uke.
    Dvs. at denne metoden fungerer litt ca i uker som inneholder helligdager
     */
    public static Integer konverterDagerTilUker(Integer antallDager) {
        return antallDager == null ? 0 : (antallDager / 5);
    }

    public static Integer parseInteger(String integer) {
        if (integer == null) {
            return null;
        }
        return Integer.parseInt(integer);
    }
}
