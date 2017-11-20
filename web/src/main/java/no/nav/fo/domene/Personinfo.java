package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@Accessors(chain = true)
@Slf4j
public class Personinfo {
    public static Map<String, String> kodeTilBeskrivelse  = new HashMap<>();
    static {
        kodeTilBeskrivelse.put("FYUS", "Fysisk utestengelse");
        kodeTilBeskrivelse.put("FTUS", "Fysisk/telefonisk utestengelse");
        kodeTilBeskrivelse.put("TOAN", "To ansatte i samtale");
    }

    private String sikkerhetstiltak;
    private boolean egenAnsatt;

    public Personinfo withSikkerhetstiltak(String sikkerhetstiltak) {
        if(Objects.isNull(sikkerhetstiltak)) {
            return this;
        }
        if(!kodeTilBeskrivelse.containsKey(sikkerhetstiltak)) {
            log.warn("Finner ikke beskrivelse for sikkerhetstiltak {}", sikkerhetstiltak);
            return this;
        }
        this.sikkerhetstiltak = kodeTilBeskrivelse.get(sikkerhetstiltak);
        return this;
    }
}
