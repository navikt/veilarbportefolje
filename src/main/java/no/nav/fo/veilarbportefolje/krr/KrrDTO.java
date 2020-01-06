package no.nav.fo.veilarbportefolje.krr;

import lombok.Value;

import java.util.Map;

@Value
public class KrrDTO {
    private Map<String, KrrKontaktInfoDTO> kontaktinfo;
}
