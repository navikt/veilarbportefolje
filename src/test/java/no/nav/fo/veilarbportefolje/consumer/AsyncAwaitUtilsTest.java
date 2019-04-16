package no.nav.fo.veilarbportefolje.consumer;

import lombok.SneakyThrows;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static no.nav.common.utils.CollectionUtils.listOf;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.async;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AsyncAwaitUtilsTest {

    private static List<Integer> list = new ArrayList<>();

    @Test
    public void skal_fortsette_eksekvering_etter_at_jobber_er_ferdig() {
        CompletableFuture future = async(listOf(() -> {
        }));

        await(future);
        assertThat(true).isTrue();
    }

    @Test
    public void skal_ikke_kortslutte_ved_feilende_element_midt_i_liste() {
        List<Runnable> jobs = listOf("1", "2", "3", "4", null, "6", "7").stream()
                .map(AsyncAwaitUtilsTest::parseAndAdd)
                .collect(toList());

        CompletableFuture future = async(jobs);
        await(future);

        int expectedSize = jobs.size() - 1;

        assertThat(list.size()).isEqualTo(expectedSize);
        assertThat(list.contains(7)).isTrue();
    }

    @SneakyThrows
    private static Runnable parseAndAdd(String x) {
        return () -> {
            int i = Integer.parseInt(x);
            list.add(i);
        };
    }

}