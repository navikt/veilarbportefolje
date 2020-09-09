package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;

import static no.nav.pto.veilarbportefolje.oppfolgingfeed.OppfolgingerfeedConfig.SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA;
import static no.nav.pto.veilarbportefolje.oppfolgingfeed.OppfolgingerfeedConfig.nesteId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingerfeedConfigTest {

    private JdbcTemplate db = mock(JdbcTemplate.class);

    private List<Map<String, Object>> queryMap(String columnName, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(columnName, value);
        return Arrays.asList(map);
    }

    @Test
    public void skalHenteSisteIdNumerisk() {
        when(db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA))
            .thenReturn(queryMap("oppfolging_sist_oppdatert_id", BigDecimal.valueOf(50)));
        assertThat(nesteId(db)).isEqualTo("51");
    }

    @Test
    public void skalHandtereAtSisteIdErNull() {
        when(db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA))
            .thenReturn(queryMap("oppfolging_sist_oppdatert_id", null));
        assertThat(nesteId(db)).isEqualTo("1");
    }

    @Test
    public void skalHandtereAtMetadataRadIkkeFinnes() {
        when(db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA))
            .thenReturn(Collections.emptyList());
        assertThat(nesteId(db)).isEqualTo("1");
    }
}
