package no.nav.pto.veilarbportefolje.domene;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OppfolgingEnhetPageDTO {
    int page_number;
    long page_number_total;
    int number_of_users;
    List<OppfolgingEnhetDTO> users;
}
