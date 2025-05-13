package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class EnsligForsorgerResponseDto {
    Long vedtakId;
    String personIdent;
    List<Barn> barn;
    Stønadstype stønadstype;
    List<Periode> periode;
    Vedtaksresultat vedtaksresultat;
}
