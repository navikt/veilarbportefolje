package no.nav.fo.veilarbportefolje.domene;

import lombok.Value;

import java.util.List;

@Value
public class OppfolgingEnhetPageDTO {
    int page_number;
    int page_number_total;
    List<OppfolgingEnhetDTO> users;
}
