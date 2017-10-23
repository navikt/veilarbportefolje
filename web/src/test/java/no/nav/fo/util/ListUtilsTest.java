package no.nav.fo.util;


import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.util.ListUtils.distinctByPropertyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ListUtilsTest {

    @Test
    public void skalLageListeMedDistinkteElementer() {
        List<Tuple2<String,String>> list1 = new ArrayList<>();
        list1.add(Tuple.of("tuple", "tuple"));

        List<Tuple2<String,String>> list2 = new ArrayList<>();
        list2.add(Tuple.of("tuple", "tuple"));

        List<Tuple2<String, String>> result = distinctByPropertyList(Tuple2::_1, list1, list2);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void skalLageListeMedAlleElementer() {
        List<Tuple2<String,String>> list1 = new ArrayList<>();
        list1.add(Tuple.of("tuple", "tuple"));

        List<Tuple2<String,String>> list2 = new ArrayList<>();
        list2.add(Tuple.of("tuple1", "tuple1"));

        List<Tuple2<String, String>> result = distinctByPropertyList(Tuple2::_1, list1, list2);
        assertThat(result.size()).isEqualTo(2);
    }

}