/**
 * Autogenerated by Avro
 * <p>
 * DO NOT EDIT DIRECTLY
 */
package no.nav.paw.arbeidssokerregisteret.api.v1;

@org.apache.avro.specific.AvroGenerated
public interface Endring {
    public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"Endring\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"types\":[{\"type\":\"enum\",\"name\":\"BrukerType\",\"symbols\":[\"UKJENT_VERDI\",\"UDEFINERT\",\"VEILEDER\",\"SYSTEM\",\"SLUTTBRUKER\"],\"default\":\"UKJENT_VERDI\"},{\"type\":\"record\",\"name\":\"Bruker\",\"doc\":\"En bruker er en person eller et system. Personer kan være sluttbrukere eller veiledere.\",\"fields\":[{\"name\":\"type\",\"type\":\"BrukerType\",\"doc\":\"Angir hvilken type bruker det er snakk om\"},{\"name\":\"id\",\"type\":\"string\",\"doc\":\"Brukerens identifikator.\\nFor sluttbruker er dette typisk fødselsnummer eller D-nummer.\\nFor system vil det rett og slett være navnet på et system, eventuelt med versjonsnummer i tillegg (APP_NAVN:VERSJON).\\nFor veileder vil det være NAV identen til veilederen.\"}]},{\"type\":\"record\",\"name\":\"Metadata\",\"doc\":\"Inneholder metadata om en endring i arbeidssøkerregisteret.\",\"fields\":[{\"name\":\"tidspunkt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Tidspunkt for endringen.\"},{\"name\":\"utfoertAv\",\"type\":\"Bruker\"},{\"name\":\"kilde\",\"type\":\"string\",\"doc\":\"Navn på systemet som utførte endringen eller ble benyttet til å utføre endringen.\"},{\"name\":\"aarsak\",\"type\":\"string\",\"doc\":\"Aarasek til endringen. Feks \\\"Flyttet ut av landet\\\" eller lignende.\"}]}],\"messages\":{}}");

    @org.apache.avro.specific.AvroGenerated
    public interface Callback extends Endring {
        public static final org.apache.avro.Protocol PROTOCOL = Endring.PROTOCOL;
    }
}