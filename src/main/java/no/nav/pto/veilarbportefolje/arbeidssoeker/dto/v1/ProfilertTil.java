/**
 * Autogenerated by Avro
 * <p>
 * DO NOT EDIT DIRECTLY
 */
package no.nav.pto.veilarbportefolje.arbeidssoeker.dto.v1;

/**
 * Resultatet av en profilering
 * UKJENT_VERDI					- 	Verdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker.
 * UDEFINERT					- 	Ingen verdi er satt.
 * ANTATT_GODE_MULIGHETER		- 	Antatt gode muligheter for å komme i arbeid.
 * ANTATT_BEHOV_FOR_VEILEDNING	- 	Antatt behov for veiledning.
 * OPPGITT_HINDRINGER			- 	Personen har oppgitt at det finnes hindringer (helse eller annet) for å komme i arbeid.
 */
@org.apache.avro.specific.AvroGenerated
public enum ProfilertTil implements org.apache.avro.generic.GenericEnumSymbol<ProfilertTil> {
    UKJENT_VERDI, UDEFINERT, ANTATT_GODE_MULIGHETER, ANTATT_BEHOV_FOR_VEILEDNING, OPPGITT_HINDRINGER;
    public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"ProfilertTil\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"doc\":\"Resultatet av en profilering\\nUKJENT_VERDI\\t\\t\\t\\t\\t- \\tVerdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker.\\nUDEFINERT\\t\\t\\t\\t\\t- \\tIngen verdi er satt.\\nANTATT_GODE_MULIGHETER\\t\\t- \\tAntatt gode muligheter for å komme i arbeid.\\nANTATT_BEHOV_FOR_VEILEDNING\\t- \\tAntatt behov for veiledning.\\nOPPGITT_HINDRINGER\\t\\t\\t- \\tPersonen har oppgitt at det finnes hindringer (helse eller annet) for å komme i arbeid.\",\"symbols\":[\"UKJENT_VERDI\",\"UDEFINERT\",\"ANTATT_GODE_MULIGHETER\",\"ANTATT_BEHOV_FOR_VEILEDNING\",\"OPPGITT_HINDRINGER\"],\"default\":\"UKJENT_VERDI\"}");

    public static org.apache.avro.Schema getClassSchema() {
        return SCHEMA$;
    }

    @Override
    public org.apache.avro.Schema getSchema() {
        return SCHEMA$;
    }
}
