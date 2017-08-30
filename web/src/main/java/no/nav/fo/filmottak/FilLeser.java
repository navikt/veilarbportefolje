package no.nav.fo.filmottak;

import io.vavr.control.Try;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public interface FilLeser {
    default Try<InputStream> lesFil(File file) {
        return Try.of(() -> {
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ByteArrayInputStream(bytes);
        });
    }
}
