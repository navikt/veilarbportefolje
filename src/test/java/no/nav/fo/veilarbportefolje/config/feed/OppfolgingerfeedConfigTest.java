package no.nav.fo.veilarbportefolje.config.feed;

import static no.nav.fo.veilarbportefolje.config.feed.OppfolgingerfeedConfig.finnSisteIdNumerisk;
import static no.nav.fo.veilarbportefolje.config.feed.OppfolgingerfeedConfig.finnSisteIdTidspunkt;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class OppfolgingerfeedConfigTest {

    private JdbcTemplate db = mock(JdbcTemplate.class);

    @Test
    public void skalHenteSisteIdTidspunkt() {
        when(db.queryForList(OppfolgingerfeedConfig.SELECT_OPPFOLGING_SIST_OPPDATERT_FROM_METADATA))
            .thenReturn(queryMap("oppfolging_sist_oppdatert", Timestamp.valueOf("2018-12-10 00:00:00")));
        assertThat(finnSisteIdTidspunkt(db), startsWith("2018-"));
    }

    private List<Map<String, Object>> queryMap(String columnName, Object value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(columnName, value);
        return Arrays.asList(map);
    }

    @Test
    public void skalHenteSisteIdNumerisk() {
        when(db.queryForList(OppfolgingerfeedConfig.SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA))
            .thenReturn(queryMap("oppfolging_sist_oppdatert_id", BigDecimal.valueOf(50)));
        assertThat(finnSisteIdNumerisk(db), Matchers.is("50"));
    }

}
