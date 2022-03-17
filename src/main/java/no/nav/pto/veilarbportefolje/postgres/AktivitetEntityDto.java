package no.nav.pto.veilarbportefolje.postgres;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class AktivitetEntityDto {
    AktivitetsType aktivitetsType;
    String muligTiltaksNavn; // Er kun satt for aktiviteter lagret i tiltaks tabellen
    Timestamp utlop;
    Timestamp start;

    public static void leggTilAktivitetPaResultat(AktorId aktoerId, AktivitetEntityDto aktivitet, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        Optional.ofNullable(result.get(aktoerId)).ifPresentOrElse(
                liste -> liste.add(aktivitet),
                () -> {
                    ArrayList<AktivitetEntityDto> liste = new ArrayList<>();
                    liste.add(aktivitet);
                    result.put(aktoerId, liste);
                }
        );
    }

    @SneakyThrows
    public static AktivitetEntityDto mapTiltakTilEntity(ResultSet rs) {
        return new AktivitetEntityDto()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setMuligTiltaksNavn(rs.getString("tiltakskode"))
                .setAktivitetsType(AktivitetsType.tiltak);
    }

    @SneakyThrows
    public static Optional<AktivitetEntityDto> mapAktivitetTilEntity(ResultSet rs) {
        String type = rs.getString("aktivitettype");
        if (!AktivitetsType.contains(type)) {
            // Noen aktiviteter skal ikke vises i oversikten: samtalereferat
            return Optional.empty();
        }
        return Optional.of(
                new AktivitetEntityDto()
                        .setStart(rs.getTimestamp("fradato"))
                        .setUtlop(rs.getTimestamp("tildato"))
                        .setAktivitetsType(AktivitetsType.valueOf(type))
        );
    }

    @SneakyThrows
    public static AktivitetEntityDto mapGruppeAktivitetTilEntity(ResultSet rs) {
        return new AktivitetEntityDto()
                .setStart(rs.getTimestamp("moteplan_startdato"))
                .setUtlop(rs.getTimestamp("moteplan_sluttdato"))
                .setAktivitetsType(AktivitetsType.gruppeaktivitet);
    }
}
