package no.nav.pto.veilarbportefolje.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {

    private static final int RESULT_VALUE = 1;
    private static final int FALLBACK_VALUE = 2;

    @Test
    public void skal_returnere_err_ved_exception() {
        Result<Integer> result = errResult();
        assertThat(result.isErr()).isTrue();
    }

    @Test
    public void skal_returnere_ok_ved_suksess() {
        Result<Integer> result = okResult();
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void skal_returnere_riktige_optionals_ved_unwrapping_av_err() {
        Result<Integer> result = errResult();
        assertThat(result.err().isPresent()).isTrue();
        assertThat(result.ok().isPresent()).isFalse();
    }

    @Test
    public void skal_returnere_riktige_optionals_ved_unwrapping_av_ok() {
        Result<Integer> result = okResult();
        assertThat(result.err().isPresent()).isFalse();
        assertThat(result.ok().isPresent()).isTrue();
    }

    @Test
    public void skal_mappe_til_nytt_result() {
        Result<Integer> oldResult = okResult();
        Result<Double> newResult = oldResult.mapOk(x -> x + 1.0);

        assertThat(newResult.isOk()).isTrue();

        Double mappedValue = newResult.ok().orElseThrow(IllegalStateException::new);
        assertThat(mappedValue).isInstanceOf(Double.class);
        assertThat(mappedValue).isEqualTo(2.0);
    }

    @Test
    public void skal_hente_fallback_value_ved_err() {
        Result<Integer> result = errResult();
        Integer integer = result.orElse(FALLBACK_VALUE);
        assertThat(integer).isEqualTo(FALLBACK_VALUE);

    }

    @Test
    public void skal_hente_original_value_ved_ok() {
        Result<Integer> result = okResult();
        Integer integer = result.orElse(FALLBACK_VALUE);
        assertThat(integer).isEqualTo(RESULT_VALUE);
    }

    private Result<Integer> okResult() {
        return Result.of(() -> RESULT_VALUE);
    }

    private Result<Integer> errResult() {
        return Result.of(() -> {
            throw new IllegalStateException();
        });
    }
}