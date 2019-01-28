package no.nav.fo.veilarbportefolje.config.feed;

import static no.nav.fo.veilarbportefolje.config.feed.OppfolgingerfeedConfig.SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class OppfolgingerfeedConfigTest {

    private JdbcTemplate db = mock(JdbcTemplate.class);

    private List<Map<String, Object>> queryMap(String columnName, Object value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(columnName, value);
        return Arrays.asList(map);
    }

    @Test
    public void skalHenteSisteIdNumerisk() {
        when(db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA))
            .thenReturn(queryMap("oppfolging_sist_oppdatert_id", BigDecimal.valueOf(50)));
        assertThat(((BigDecimal) db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA).get(0).get("oppfolging_sist_oppdatert_id")).toPlainString(), is("50"));
    }

}
