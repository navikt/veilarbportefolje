package no.nav.fo.veilarbportefolje.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UtilsTest {
    @Test
    public void skal_splitte_opp_liste() {
        int size = 1024;
        ArrayList<Object> list = new ArrayList<>(size);
        IntStream.range(0, size).forEach(list::add);

        List<List<Object>> partitionedList = Utils.splittOppListe(list, 100);
        assertThat(partitionedList.size()).isEqualTo(11);
    }
}
