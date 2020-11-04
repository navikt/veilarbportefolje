package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakRepository;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.krr.KrrRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Import({
        TiltakRepository.class,
        AktivitetDAO.class,
        BrukerRepository.class,
        KrrRepository.class,
        OppfolgingRepository.class
})
public class DatabaseConfigTest {


    @Bean
    public DataSource hsqldbDataSource() {
      return TestUtil.setupInMemoryDatabase();
    }


    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager transactionManager) {
        return new Transactor(transactionManager);
    }

}
