package no.nav.pto.veilarbportefolje.util;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.StepperFacetConfig;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

public class StepperUtils {

    @Value
    public static class Step {
        int fra;
        int til;
        long verdi;
    }

    public static <T> List<Step> groupByStepping(StepperFacetConfig config, List<T> elementer, Function<T, Integer> extractor) {
        Map<Tuple2<Integer, Integer>, Long> countedFacets = elementer
                .stream()
                .map(extractor)
                .filter((Integer element) -> element >= 0 && element <= config.getMax())
                .map((Integer element) -> findStep(config.getMin(), config.getStep(), config.getMax(), element))
                .collect(groupingBy(Function.identity(), counting()));
        List<Tuple2<Integer, Integer>> allFacets = stepper(config.getMin(), config.getStep(), config.getMax());

        allFacets.forEach((facet) -> countedFacets.putIfAbsent(facet, 0L));

        return countedFacets
                .entrySet()
                .stream()
                .map((entry) -> new Step(entry.getKey()._1, entry.getKey()._2, entry.getValue()))
                .collect(toList());
    }

    public static Tuple2<Integer, Integer> findStep(int min, int stepsize, int max, int value) {
        if (value >= 0 && value < min) {
            return Tuple.of(0, min - 1);
        } else {
            int effectiveStepsize = stepsize + 1;
            int normalizedValue = value - min;
            int valueStepPosition = normalizedValue / effectiveStepsize;
            int maxStepPosition = (max - min) / effectiveStepsize;
            int stepPositon = Math.min(valueStepPosition, maxStepPosition);

            return Tuple.of(stepPositon * effectiveStepsize + min, Math.min(max, stepPositon * effectiveStepsize + min + stepsize));
        }
    }

    public static List<Tuple2<Integer, Integer>> stepper(int min, int step, int max) {
        List<Tuple2<Integer, Integer>> steps = new LinkedList<>();
        int start = 0;
        int end = min - 1;

        while (end <= max) {
            steps.add(Tuple.of(start, end));
            start = end + 1;
            end = start + step;
        }

        if (start < max) {
            steps.add(Tuple.of(start, max));
        }

        return steps;
    }
}
