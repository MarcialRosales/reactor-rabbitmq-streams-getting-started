/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package schemas.event.v8;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class Event extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 5052390207110982228L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Event\",\"namespace\":\"schemas.event.v8\",\"fields\":[{\"name\":\"timestamp\",\"type\":\"long\",\"default\":0},{\"name\":\"number\",\"type\":\"long\",\"default\":0}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<Event> ENCODER =
      new BinaryMessageEncoder<Event>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Event> DECODER =
      new BinaryMessageDecoder<Event>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<Event> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<Event> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<Event> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<Event>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this Event to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a Event from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a Event instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static Event fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  @Deprecated public long timestamp;
  @Deprecated public long number;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Event() {}

  /**
   * All-args constructor.
   * @param timestamp The new value for timestamp
   * @param number The new value for number
   */
  public Event(java.lang.Long timestamp, java.lang.Long number) {
    this.timestamp = timestamp;
    this.number = number;
  }

  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return timestamp;
    case 1: return number;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: timestamp = (java.lang.Long)value$; break;
    case 1: number = (java.lang.Long)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'timestamp' field.
   * @return The value of the 'timestamp' field.
   */
  public long getTimestamp() {
    return timestamp;
  }


  /**
   * Sets the value of the 'timestamp' field.
   * @param value the value to set.
   */
  public void setTimestamp(long value) {
    this.timestamp = value;
  }

  /**
   * Gets the value of the 'number' field.
   * @return The value of the 'number' field.
   */
  public long getNumber() {
    return number;
  }


  /**
   * Sets the value of the 'number' field.
   * @param value the value to set.
   */
  public void setNumber(long value) {
    this.number = value;
  }

  /**
   * Creates a new Event RecordBuilder.
   * @return A new Event RecordBuilder
   */
  public static schemas.event.v8.Event.Builder newBuilder() {
    return new schemas.event.v8.Event.Builder();
  }

  /**
   * Creates a new Event RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Event RecordBuilder
   */
  public static schemas.event.v8.Event.Builder newBuilder(schemas.event.v8.Event.Builder other) {
    if (other == null) {
      return new schemas.event.v8.Event.Builder();
    } else {
      return new schemas.event.v8.Event.Builder(other);
    }
  }

  /**
   * Creates a new Event RecordBuilder by copying an existing Event instance.
   * @param other The existing instance to copy.
   * @return A new Event RecordBuilder
   */
  public static schemas.event.v8.Event.Builder newBuilder(schemas.event.v8.Event other) {
    if (other == null) {
      return new schemas.event.v8.Event.Builder();
    } else {
      return new schemas.event.v8.Event.Builder(other);
    }
  }

  /**
   * RecordBuilder for Event instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Event>
    implements org.apache.avro.data.RecordBuilder<Event> {

    private long timestamp;
    private long number;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(schemas.event.v8.Event.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[0].schema(), other.timestamp);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.number)) {
        this.number = data().deepCopy(fields()[1].schema(), other.number);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
    }

    /**
     * Creates a Builder by copying an existing Event instance
     * @param other The existing instance to copy.
     */
    private Builder(schemas.event.v8.Event other) {
      super(SCHEMA$);
      if (isValidValue(fields()[0], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[0].schema(), other.timestamp);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.number)) {
        this.number = data().deepCopy(fields()[1].schema(), other.number);
        fieldSetFlags()[1] = true;
      }
    }

    /**
      * Gets the value of the 'timestamp' field.
      * @return The value.
      */
    public long getTimestamp() {
      return timestamp;
    }


    /**
      * Sets the value of the 'timestamp' field.
      * @param value The value of 'timestamp'.
      * @return This builder.
      */
    public schemas.event.v8.Event.Builder setTimestamp(long value) {
      validate(fields()[0], value);
      this.timestamp = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * @return This builder.
      */
    public schemas.event.v8.Event.Builder clearTimestamp() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'number' field.
      * @return The value.
      */
    public long getNumber() {
      return number;
    }


    /**
      * Sets the value of the 'number' field.
      * @param value The value of 'number'.
      * @return This builder.
      */
    public schemas.event.v8.Event.Builder setNumber(long value) {
      validate(fields()[1], value);
      this.number = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'number' field has been set.
      * @return True if the 'number' field has been set, false otherwise.
      */
    public boolean hasNumber() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'number' field.
      * @return This builder.
      */
    public schemas.event.v8.Event.Builder clearNumber() {
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Event build() {
      try {
        Event record = new Event();
        record.timestamp = fieldSetFlags()[0] ? this.timestamp : (java.lang.Long) defaultValue(fields()[0]);
        record.number = fieldSetFlags()[1] ? this.number : (java.lang.Long) defaultValue(fields()[1]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Event>
    WRITER$ = (org.apache.avro.io.DatumWriter<Event>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Event>
    READER$ = (org.apache.avro.io.DatumReader<Event>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeLong(this.timestamp);

    out.writeLong(this.number);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.timestamp = in.readLong();

      this.number = in.readLong();

    } else {
      for (int i = 0; i < 2; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.timestamp = in.readLong();
          break;

        case 1:
          this.number = in.readLong();
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










