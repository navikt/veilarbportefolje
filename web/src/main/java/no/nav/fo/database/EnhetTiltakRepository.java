package no.nav.fo.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.EnhetTiltak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;


public class EnhetTiltakRepository {
    private static Logger logger = LoggerFactory.getLogger(EnhetTiltakRepository.class);

    @Inject
    private JdbcTemplate db;

    public Try<EnhetTiltak> retrieveEnhettiltak(String enhet) {

        return Try.of(
                () -> db.query(retrieveSql(), new String[]{enhet}, EnhetTiltakRepository::rowMapper)
        ).onFailure(e -> logger.warn("Finner ikke tiltak for enhet med enhetid {}", enhet));
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
