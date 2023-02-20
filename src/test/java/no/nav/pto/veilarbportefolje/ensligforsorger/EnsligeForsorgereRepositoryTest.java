package no.nav.pto.veilarbportefolje.ensligforsorger;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.stream.Stream;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class EnsligeForsorgereRepositoryTest {

    @Autowired
    private EnsligeForsorgereRepository ensligeForsorgereRepository;

    @Test
    public void test() {
        Integer test = ensligeForsorgereRepository.lagreStonadstype("Test");
        Integer test1 = ensligeForsorgereRepository.lagreStonadstype("Test1");
        Integer test2 = ensligeForsorgereRepository.lagreStonadstype("Test2");
    }

    @Test
    public void testYoungestDate() {
        LocalDate date1 = LocalDate.of(2022, 3, 22);
        LocalDate date2 = LocalDate.of(2022, 5, 18);
        LocalDate date3 = LocalDate.of(2021, 2, 6);

        LocalDate newestDate = Stream.of(date1, date2, date3).max(LocalDate::compareTo).get();
        Assert.assertEquals(newestDate, date2);
    }

}