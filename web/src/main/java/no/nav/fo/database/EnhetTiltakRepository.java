package no.nav.fo.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.EnhetTiltak;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EnhetTiltakRepository {

    @Inject
    private JdbcTemplate db;

    public Try<EnhetTiltak> retrieveEnhettiltak(String enhet) {

        return Try.of(
                () -> db.query(retrieveSql(), new String[]{enhet}, EnhetTiltakRepository::rowMapper)
        ).onFailure(e -> log.warn("Finner ikke tiltak for enhet med enhetid {}", enhet));
    }

    @SneakyThrows
    private static EnhetTiltak rowMapper(ResultSet rs) {
        Map<String,String> tiltak = new HashMap<>();

        while(rs.next()) {
            tiltak.put(rs.getString("kode"), rs.getString("verdi"));
        }

        return new EnhetTiltak().setTiltak(tiltak);

    }

    private String retrieveSql() {
        return "SELECT verdi, kode FROM tiltakkodeverk JOIN enhettiltak" +
                " ON enhettiltak.tiltakskode = tiltakkodeverk.kode" +
                " WHERE enhettiltak.enhetid= ? ";
    }
}
