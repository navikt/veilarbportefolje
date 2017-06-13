package no.nav.fo.util;

import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Brukerdata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.YtelseMapping.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class UpdateBatchQueryTest {

    @Inject
    JdbcTemplate db;

    @Inject
    private BrukerRepository brukerRepository;

    @Test
    public void skalLageBatchUpdateQuery() throws Exception {
        LocalDateTime now = now();
        List<Brukerdata> brukerdata = asList(
                new Brukerdata().setPersonid("abba").setYtelse(AAP_MAXTID).setAapMaxtid(now),
                new Brukerdata().setPersonid("acdc").setYtelse(DAGPENGER_OVRIGE).setAapMaxtid(now),
                new Brukerdata().setPersonid("aedc").setYtelse(AAP_UNNTAK).setAapMaxtid(now)
        );
        List<Brukerdata> oppdatertBrukerdata = asList(
                new Brukerdata().setPersonid("abba").setYtelse(AAP_UNNTAK).setAapMaxtid(now.minusDays(1)),
                new Brukerdata().setPersonid("acdc").setYtelse(DAGPENGER_MED_PERMITTERING).setAapMaxtid(now.minusDays(1)),
                new Brukerdata().setPersonid("aedc").setYtelse(AAP_MAXTID).setAapMaxtid(now.plusDays(1))
        );


        brukerdata.forEach((bruker) -> bruker.toInsertQuery(db).execute());

        int[] ints = Brukerdata.batchUpdate(db, oppdatertBrukerdata);

        assertThat(ints).hasSize(3);
        assertThat(ints).containsExactly(1, 1, 1);

        List<Brukerdata> oppdaterteBrukere = brukerRepository.retrieveBrukerdata(brukerdata.stream().map(Brukerdata::getPersonid).collect(toList()));
        assertThat(oppdaterteBrukere.get(0).getYtelse()).isEqualTo(AAP_UNNTAK);
        assertThat(oppdaterteBrukere.get(1).getYtelse()).isEqualTo(DAGPENGER_MED_PERMITTERING);
        assertThat(oppdaterteBrukere.get(2).getYtelse()).isEqualTo(AAP_MAXTID);
    }
}