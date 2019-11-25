package no.nav.fo.veilarbportefolje.domene;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OppfolgingEnhetPageDTO {
    int page_number;
    int page_number_total;
    int number_of_users;
    List<OppfolgingEnhetDTO> users;
}
