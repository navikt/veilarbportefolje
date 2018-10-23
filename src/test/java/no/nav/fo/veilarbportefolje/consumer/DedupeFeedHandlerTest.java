package no.nav.fo.veilarbportefolje.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.fo.feed.consumer.FeedCallback;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DedupeFeedHandlerTest {

    @Test
    public void stopper_like_repeterende_kall() {
        FeedCallback<DomainObject> mock = mock(FeedCallback.class);
        FeedCallback<DomainObject> deduped = DedupeFeedHandler.of(2, mock);

        List<DomainObject> list = asList(
                new DomainObject("navn 1", 0),
                new DomainObject("navn 2", 0)
        );

        deduped.call("string", list);
        deduped.call("string", list);

        verify(mock, times(1)).call(anyString(), anyList());
    }


    @Test
    public void fjerner_duplikater() {
        FeedCallback<DomainObject> mock = mock(FeedCallback.class);
        FeedCallback<DomainObject> deduped = DedupeFeedHandler.of(2, mock);

        List<DomainObject> list = asList(
                new DomainObject("navn 1", 0)
        );

        List<DomainObject> list2 = asList(
                new DomainObject("navn 1", 0),
                new DomainObject("navn 2", 0)

        );


        deduped.call("string", list);
        deduped.call("string", list2);
        deduped.call("string", list2);

        ArgumentCaptor<List<DomainObject>> captor = ArgumentCaptor.forClass(List.class);

        verify(mock, times(2)).call(anyString(), captor.capture());

        List<List<DomainObject>> allValues = captor.getAllValues();
        assertThat(allValues.get(0).size()).isEqualTo(1);
        assertThat(allValues.get(1).size()).isEqualTo(1);
    }


    @Test
    public void skal_ha_maks_n_elementer() {
        FeedCallback<DomainObject> mock = mock(FeedCallback.class);
        DedupeFeedHandler<DomainObject> deduped = DedupeFeedHandler.of(3, mock);

        List<DomainObject> list = asList(
                new DomainObject("navn 1", 0),
                new DomainObject("navn 2", 0),
                new DomainObject("navn 3", 0)

        );

        List<DomainObject> list2 = asList(
                new DomainObject("navn 1", 0),
                new DomainObject("navn 4", 0)

        );


        deduped.call("string", list);
        deduped.call("string", list2);


        verify(mock, times(2)).call(anyString(), anyList());
        assertThat(deduped.getList()).containsAll(list2);
        assertThat(deduped.size()).isEqualTo(3);
    }


    @Data
    @AllArgsConstructor
    public class DomainObject {
        String navn;
        int alder;
    }
}
