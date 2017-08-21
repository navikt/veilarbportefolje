package no.nav.fo.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.EnhetTiltak;
import no.nav.fo.util.sql.where.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.util.sql.SqlUtils.*;

public class EnhetTiltakRepository {
    private static Logger LOG = LoggerFactory.getLogger(EnhetTiltakRepository.class);

    @Inject
    private JdbcTemplate db;

    @Inject
    private DataSource ds;


    public EnhetTiltak retrieveEnhettiltak(String enhet) {

        List<String> liste = db.query(retrieveSql(), new String[]{enhet}, EnhetTiltakRepository::rowMapper);

        if (liste == null) {
            liste = new ArrayList<>();
        }

        return new EnhetTiltak().setEnhet(enhet).setTiltak(liste);

    }

    @SneakyThrows
    private static List<String> rowMapper(ResultSet rs) {
        List<String> tiltak = new ArrayList<>();

        while(rs.next()) {
            tiltak.add(rs.getString("verdi"));
        }

        return tiltak;

    }

    public String retrieveSql() {
        return "SELECT verdi FROM tiltakkodeverk JOIN enhettiltak" +
                " ON enhettiltak.tiltakkode = tiltakkodeverk.kode" +
                "WHERE enhettiltak.enhetid= ? ";
    }
}
