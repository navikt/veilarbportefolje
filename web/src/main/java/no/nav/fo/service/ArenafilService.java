package no.nav.fo.service;

import javaslang.control.Try;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public interface ArenafilService {
    default Try<InputStream> hentArenafil(File file) {
        return Try.of(() -> {
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ByteArrayInputStream(bytes);
        });
    }
}
