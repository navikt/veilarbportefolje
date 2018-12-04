package no.nav.fo.veilarbportefolje.util;

import java.util.ArrayList;
import java.util.List;

public class Utils {


    /**
     * Splitt opp liste i partisjoner av størrelse partitionSize. En liste med 10 elementer og partitionSize 2 vil da se
     * slik ut:
     *
     * [1,2,3,4,5,6,7,8,9] => [[0,1],[2,3],[4,5],[6,7],[8,9]]
     *
     * Merk at siste element vil kunne inneholde færre enn partitionSize elementer.
     *
     */
    public static <T> List<List<T>> splittOppListe(List<T> list, int partitionSize) {
        List<List<T>> slicedList = new ArrayList<>();
        int listSize = list.size();
        for (int i = 0; i < listSize; i = i + partitionSize) {
            int toIndex = i + partitionSize;
            if (toIndex > listSize) {
                toIndex = listSize;
            }
            List<T> sublist = list.subList(i, toIndex);
            slicedList.add(sublist);
        }
        return slicedList;
    }
}
