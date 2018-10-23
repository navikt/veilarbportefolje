package no.nav.fo.veilarbportefolje.util;

import org.junit.Test;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class BatchConsumerTest {

    @Test
    public void skalBatcheKallTilConsumer() throws Exception {
        List<Integer> data = IntStream.range(1, 100).boxed().collect(toList());
        final int[] dataSize = {data.size()};

        BatchConsumer<Integer> consumer = BatchConsumer.batchConsumer(7, (listeAvTall) -> {
            assertThat(listeAvTall.size()).isLessThanOrEqualTo(7);
            dataSize[0] -= listeAvTall.size();
        });

        data.forEach(consumer);
        consumer.flush();
        assertThat(dataSize[0]).isEqualTo(0);
    }
}
