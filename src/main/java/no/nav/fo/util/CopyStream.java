package no.nav.fo.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class CopyStream extends ByteArrayOutputStream{

    public CopyStream() {
    }

    public CopyStream(int size) {
        super(size);
    }

    public InputStream toInputStream() {
        return new ByteArrayInputStream(this.buf, 0, this.count);
    }

}
