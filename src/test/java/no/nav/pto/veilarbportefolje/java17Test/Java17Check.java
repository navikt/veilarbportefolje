package no.nav.pto.veilarbportefolje.java17Test;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class Java17Check {
    record Person(String name, String occupation) { }
    @Test
    public void record() {
        Person p1 = new Person("test1", "software engineer");

        assertThat(p1.name()).isEqualTo("test1");
        assertThat(p1.occupation()).isEqualTo("software engineer");
    }

}
