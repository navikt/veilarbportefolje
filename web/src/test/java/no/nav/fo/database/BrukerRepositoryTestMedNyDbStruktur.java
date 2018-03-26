package no.nav.fo.database;

import org.junit.Before;

/**
 * Kjører alle testene fra BrukerRepositoryTest på nøyaktig sammt måte, men med
 * nyStrukturForIndeksering = true slik at nye tabeller og views er datagrunnlag
 *
 */
public class BrukerRepositoryTestMedNyDbStruktur extends BrukerRepositoryTest {

    @Before
    public void setNyDatastruktur() {
        brukerRepository.nyStrukturForIndeksering = true;
    }    

}
