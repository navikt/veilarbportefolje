package no.nav.fo.veilarbportefolje.util.sql.where;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class WhereClauseTest {
    @Test
    public void appliesToTest() {
        assertThat(WhereClause.equals("felt1", 0).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt2", 0).appliesTo("felt1")).isFalse();

        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt2")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt3")).isFalse();

        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt2")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt3")).isFalse();
    }
}
