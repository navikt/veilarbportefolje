/**
 * Autogenerated by Avro
 * <p>
 * DO NOT EDIT DIRECTLY
 */
package no.nav.paw.arbeidssokerregisteret.api.v2;

import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.SchemaStore;
import org.apache.avro.specific.SpecificData;

@org.apache.avro.specific.AvroGenerated
public class Annet extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
    private static final long serialVersionUID = -5932453260863584591L;


    public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Annet\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v2\",\"fields\":[{\"name\":\"andreForholdHindrerArbeid\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"JaNeiVetIkke\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"doc\":\"Enkel enum som brukes til typisk 'ja', 'nei' eller 'vet ikke' svar.\",\"symbols\":[\"JA\",\"NEI\",\"VET_IKKE\"]}],\"default\":null}]}");

    public static org.apache.avro.Schema getClassSchema() {
        return SCHEMA$;
    }

    private static final SpecificData MODEL$ = new SpecificData();

    private static final BinaryMessageEncoder<Annet> ENCODER =
            new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

    private static final BinaryMessageDecoder<Annet> DECODER =
            new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

    /**
     * Return the BinaryMessageEncoder instance used by this class.
     *
     * @return the message encoder used by this class
     */
    public static BinaryMessageEncoder<Annet> getEncoder() {
        return ENCODER;
    }

    /**
     * Return the BinaryMessageDecoder instance used by this class.
     *
     * @return the message decoder used by this class
     */
    public static BinaryMessageDecoder<Annet> getDecoder() {
        return DECODER;
    }

    /**
     * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
     *
     * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
     * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
     */
    public static BinaryMessageDecoder<Annet> createDecoder(SchemaStore resolver) {
        return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
    }

    /**
     * Serializes this Annet to a ByteBuffer.
     *
     * @return a buffer holding the serialized data for this instance
     * @throws java.io.IOException if this instance could not be serialized
     */
    public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
        return ENCODER.encode(this);
    }

    /**
     * Deserializes a Annet from a ByteBuffer.
     *
     * @param b a byte buffer holding serialized data for an instance of this class
     * @return a Annet instance decoded from the given buffer
     * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
     */
    public static Annet fromByteBuffer(
            java.nio.ByteBuffer b) throws java.io.IOException {
        return DECODER.decode(b);
    }

    private JaNeiVetIkke andreForholdHindrerArbeid;

    /**
     * Default constructor.  Note that this does not initialize fields
     * to their default values from the schema.  If that is desired then
     * one should use <code>newBuilder()</code>.
     */
    public Annet() {
    }

    /**
     * All-args constructor.
     *
     * @param andreForholdHindrerArbeid The new value for andreForholdHindrerArbeid
     */
    public Annet(JaNeiVetIkke andreForholdHindrerArbeid) {
        this.andreForholdHindrerArbeid = andreForholdHindrerArbeid;
    }

    @Override
    public SpecificData getSpecificData() {
        return MODEL$;
    }

    @Override
    public org.apache.avro.Schema getSchema() {
        return SCHEMA$;
    }

    // Used by DatumWriter.  Applications should not call.
    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return andreForholdHindrerArbeid;
            default:
                throw new IndexOutOfBoundsException("Invalid index: " + field$);
        }
    }

    // Used by DatumReader.  Applications should not call.
    @Override
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                andreForholdHindrerArbeid = (JaNeiVetIkke) value$;
                break;
            default:
                throw new IndexOutOfBoundsException("Invalid index: " + field$);
        }
    }

    /**
     * Gets the value of the 'andreForholdHindrerArbeid' field.
     *
     * @return The value of the 'andreForholdHindrerArbeid' field.
     */
    public JaNeiVetIkke getAndreForholdHindrerArbeid() {
        return andreForholdHindrerArbeid;
    }


    /**
     * Sets the value of the 'andreForholdHindrerArbeid' field.
     *
     * @param value the value to set.
     */
    public void setAndreForholdHindrerArbeid(JaNeiVetIkke value) {
        this.andreForholdHindrerArbeid = value;
    }

    /**
     * Creates a new Annet RecordBuilder.
     *
     * @return A new Annet RecordBuilder
     */
    public static Annet.Builder newBuilder() {
        return new Annet.Builder();
    }

    /**
     * Creates a new Annet RecordBuilder by copying an existing Builder.
     *
     * @param other The existing builder to copy.
     * @return A new Annet RecordBuilder
     */
    public static Annet.Builder newBuilder(Annet.Builder other) {
        if (other == null) {
            return new Annet.Builder();
        } else {
            return new Annet.Builder(other);
        }
    }

    /**
     * Creates a new Annet RecordBuilder by copying an existing Annet instance.
     *
     * @param other The existing instance to copy.
     * @return A new Annet RecordBuilder
     */
    public static Annet.Builder newBuilder(Annet other) {
        if (other == null) {
            return new Annet.Builder();
        } else {
            return new Annet.Builder(other);
        }
    }

    /**
     * RecordBuilder for Annet instances.
     */
    @org.apache.avro.specific.AvroGenerated
    public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Annet>
            implements org.apache.avro.data.RecordBuilder<Annet> {

        private JaNeiVetIkke andreForholdHindrerArbeid;

        /**
         * Creates a new Builder
         */
        private Builder() {
            super(SCHEMA$, MODEL$);
        }

        /**
         * Creates a Builder by copying an existing Builder.
         *
         * @param other The existing Builder to copy.
         */
        private Builder(Annet.Builder other) {
            super(other);
            if (isValidValue(fields()[0], other.andreForholdHindrerArbeid)) {
                this.andreForholdHindrerArbeid = data().deepCopy(fields()[0].schema(), other.andreForholdHindrerArbeid);
                fieldSetFlags()[0] = other.fieldSetFlags()[0];
            }
        }

        /**
         * Creates a Builder by copying an existing Annet instance
         *
         * @param other The existing instance to copy.
         */
        private Builder(Annet other) {
            super(SCHEMA$, MODEL$);
            if (isValidValue(fields()[0], other.andreForholdHindrerArbeid)) {
                this.andreForholdHindrerArbeid = data().deepCopy(fields()[0].schema(), other.andreForholdHindrerArbeid);
                fieldSetFlags()[0] = true;
            }
        }

        /**
         * Gets the value of the 'andreForholdHindrerArbeid' field.
         *
         * @return The value.
         */
        public JaNeiVetIkke getAndreForholdHindrerArbeid() {
            return andreForholdHindrerArbeid;
        }


        /**
         * Sets the value of the 'andreForholdHindrerArbeid' field.
         *
         * @param value The value of 'andreForholdHindrerArbeid'.
         * @return This builder.
         */
        public Annet.Builder setAndreForholdHindrerArbeid(JaNeiVetIkke value) {
            validate(fields()[0], value);
            this.andreForholdHindrerArbeid = value;
            fieldSetFlags()[0] = true;
            return this;
        }

        /**
         * Checks whether the 'andreForholdHindrerArbeid' field has been set.
         *
         * @return True if the 'andreForholdHindrerArbeid' field has been set, false otherwise.
         */
        public boolean hasAndreForholdHindrerArbeid() {
            return fieldSetFlags()[0];
        }


        /**
         * Clears the value of the 'andreForholdHindrerArbeid' field.
         *
         * @return This builder.
         */
        public Annet.Builder clearAndreForholdHindrerArbeid() {
            andreForholdHindrerArbeid = null;
            fieldSetFlags()[0] = false;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Annet build() {
            try {
                Annet record = new Annet();
                record.andreForholdHindrerArbeid = fieldSetFlags()[0] ? this.andreForholdHindrerArbeid : (JaNeiVetIkke) defaultValue(fields()[0]);
                return record;
            } catch (org.apache.avro.AvroMissingFieldException e) {
                throw e;
            } catch (Exception e) {
                throw new org.apache.avro.AvroRuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumWriter<Annet>
            WRITER$ = (org.apache.avro.io.DatumWriter<Annet>) MODEL$.createDatumWriter(SCHEMA$);

    @Override
    public void writeExternal(java.io.ObjectOutput out)
            throws java.io.IOException {
        WRITER$.write(this, SpecificData.getEncoder(out));
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumReader<Annet>
            READER$ = (org.apache.avro.io.DatumReader<Annet>) MODEL$.createDatumReader(SCHEMA$);

    @Override
    public void readExternal(java.io.ObjectInput in)
            throws java.io.IOException {
        READER$.read(this, SpecificData.getDecoder(in));
    }

    @Override
    protected boolean hasCustomCoders() {
        return true;
    }

    @Override
    public void customEncode(org.apache.avro.io.Encoder out)
            throws java.io.IOException {
        if (this.andreForholdHindrerArbeid == null) {
            out.writeIndex(0);
            out.writeNull();
        } else {
            out.writeIndex(1);
            out.writeEnum(this.andreForholdHindrerArbeid.ordinal());
        }

    }

    @Override
    public void customDecode(org.apache.avro.io.ResolvingDecoder in)
            throws java.io.IOException {
        org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
        if (fieldOrder == null) {
            if (in.readIndex() != 1) {
                in.readNull();
                this.andreForholdHindrerArbeid = null;
            } else {
                this.andreForholdHindrerArbeid = JaNeiVetIkke.values()[in.readEnum()];
            }

        } else {
            for (int i = 0; i < 1; i++) {
                switch (fieldOrder[i].pos()) {
                    case 0:
                        if (in.readIndex() != 1) {
                            in.readNull();
                            this.andreForholdHindrerArbeid = null;
                        } else {
                            this.andreForholdHindrerArbeid = JaNeiVetIkke.values()[in.readEnum()];
                        }
                        break;

                    default:
                        throw new java.io.IOException("Corrupt ResolvingDecoder.");
                }
            }
        }
    }
}










