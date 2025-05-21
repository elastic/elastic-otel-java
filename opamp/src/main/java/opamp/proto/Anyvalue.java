/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package opamp.proto;

public final class Anyvalue {
  private Anyvalue() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public interface AnyValueOrBuilder
      extends
      // @@protoc_insertion_point(interface_extends:opamp.proto.AnyValue)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string string_value = 1;</code>
     *
     * @return Whether the stringValue field is set.
     */
    boolean hasStringValue();

    /**
     * <code>string string_value = 1;</code>
     *
     * @return The stringValue.
     */
    String getStringValue();

    /**
     * <code>string string_value = 1;</code>
     *
     * @return The bytes for stringValue.
     */
    com.google.protobuf.ByteString getStringValueBytes();

    /**
     * <code>bool bool_value = 2;</code>
     *
     * @return Whether the boolValue field is set.
     */
    boolean hasBoolValue();

    /**
     * <code>bool bool_value = 2;</code>
     *
     * @return The boolValue.
     */
    boolean getBoolValue();

    /**
     * <code>int64 int_value = 3;</code>
     *
     * @return Whether the intValue field is set.
     */
    boolean hasIntValue();

    /**
     * <code>int64 int_value = 3;</code>
     *
     * @return The intValue.
     */
    long getIntValue();

    /**
     * <code>double double_value = 4;</code>
     *
     * @return Whether the doubleValue field is set.
     */
    boolean hasDoubleValue();

    /**
     * <code>double double_value = 4;</code>
     *
     * @return The doubleValue.
     */
    double getDoubleValue();

    /**
     * <code>.opamp.proto.ArrayValue array_value = 5;</code>
     *
     * @return Whether the arrayValue field is set.
     */
    boolean hasArrayValue();

    /**
     * <code>.opamp.proto.ArrayValue array_value = 5;</code>
     *
     * @return The arrayValue.
     */
    ArrayValue getArrayValue();

    /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
    ArrayValueOrBuilder getArrayValueOrBuilder();

    /**
     * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
     *
     * @return Whether the kvlistValue field is set.
     */
    boolean hasKvlistValue();

    /**
     * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
     *
     * @return The kvlistValue.
     */
    KeyValueList getKvlistValue();

    /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
    KeyValueListOrBuilder getKvlistValueOrBuilder();

    /**
     * <code>bytes bytes_value = 7;</code>
     *
     * @return Whether the bytesValue field is set.
     */
    boolean hasBytesValue();

    /**
     * <code>bytes bytes_value = 7;</code>
     *
     * @return The bytesValue.
     */
    com.google.protobuf.ByteString getBytesValue();

    AnyValue.ValueCase getValueCase();
  }

  /**
   *
   *
   * <pre>
   * AnyValue is used to represent any type of attribute value. AnyValue may contain a
   * primitive value such as a string or integer or it may contain an arbitrary nested
   * object containing arrays, key-value lists and primitives.
   * </pre>
   *
   * Protobuf type {@code opamp.proto.AnyValue}
   */
  public static final class AnyValue extends com.google.protobuf.GeneratedMessageV3
      implements
      // @@protoc_insertion_point(message_implements:opamp.proto.AnyValue)
      AnyValueOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use AnyValue.newBuilder() to construct.
    private AnyValue(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private AnyValue() {}

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
      return new AnyValue();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return Anyvalue.internal_static_opamp_proto_AnyValue_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return Anyvalue.internal_static_opamp_proto_AnyValue_fieldAccessorTable
          .ensureFieldAccessorsInitialized(AnyValue.class, Builder.class);
    }

    private int valueCase_ = 0;

    @SuppressWarnings("serial")
    private Object value_;

    public enum ValueCase implements com.google.protobuf.Internal.EnumLite, InternalOneOfEnum {
      STRING_VALUE(1),
      BOOL_VALUE(2),
      INT_VALUE(3),
      DOUBLE_VALUE(4),
      ARRAY_VALUE(5),
      KVLIST_VALUE(6),
      BYTES_VALUE(7),
      VALUE_NOT_SET(0);
      private final int value;

      private ValueCase(int value) {
        this.value = value;
      }

      /**
       * @param value The number of the enum to look for.
       * @return The enum associated with the given number.
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @Deprecated
      public static ValueCase valueOf(int value) {
        return forNumber(value);
      }

      public static ValueCase forNumber(int value) {
        switch (value) {
          case 1:
            return STRING_VALUE;
          case 2:
            return BOOL_VALUE;
          case 3:
            return INT_VALUE;
          case 4:
            return DOUBLE_VALUE;
          case 5:
            return ARRAY_VALUE;
          case 6:
            return KVLIST_VALUE;
          case 7:
            return BYTES_VALUE;
          case 0:
            return VALUE_NOT_SET;
          default:
            return null;
        }
      }

      public int getNumber() {
        return this.value;
      }
    };

    public ValueCase getValueCase() {
      return ValueCase.forNumber(valueCase_);
    }

    public static final int STRING_VALUE_FIELD_NUMBER = 1;

    /**
     * <code>string string_value = 1;</code>
     *
     * @return Whether the stringValue field is set.
     */
    public boolean hasStringValue() {
      return valueCase_ == 1;
    }

    /**
     * <code>string string_value = 1;</code>
     *
     * @return The stringValue.
     */
    public String getStringValue() {
      Object ref = "";
      if (valueCase_ == 1) {
        ref = value_;
      }
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (valueCase_ == 1) {
          value_ = s;
        }
        return s;
      }
    }

    /**
     * <code>string string_value = 1;</code>
     *
     * @return The bytes for stringValue.
     */
    public com.google.protobuf.ByteString getStringValueBytes() {
      Object ref = "";
      if (valueCase_ == 1) {
        ref = value_;
      }
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        if (valueCase_ == 1) {
          value_ = b;
        }
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int BOOL_VALUE_FIELD_NUMBER = 2;

    /**
     * <code>bool bool_value = 2;</code>
     *
     * @return Whether the boolValue field is set.
     */
    @Override
    public boolean hasBoolValue() {
      return valueCase_ == 2;
    }

    /**
     * <code>bool bool_value = 2;</code>
     *
     * @return The boolValue.
     */
    @Override
    public boolean getBoolValue() {
      if (valueCase_ == 2) {
        return (Boolean) value_;
      }
      return false;
    }

    public static final int INT_VALUE_FIELD_NUMBER = 3;

    /**
     * <code>int64 int_value = 3;</code>
     *
     * @return Whether the intValue field is set.
     */
    @Override
    public boolean hasIntValue() {
      return valueCase_ == 3;
    }

    /**
     * <code>int64 int_value = 3;</code>
     *
     * @return The intValue.
     */
    @Override
    public long getIntValue() {
      if (valueCase_ == 3) {
        return (Long) value_;
      }
      return 0L;
    }

    public static final int DOUBLE_VALUE_FIELD_NUMBER = 4;

    /**
     * <code>double double_value = 4;</code>
     *
     * @return Whether the doubleValue field is set.
     */
    @Override
    public boolean hasDoubleValue() {
      return valueCase_ == 4;
    }

    /**
     * <code>double double_value = 4;</code>
     *
     * @return The doubleValue.
     */
    @Override
    public double getDoubleValue() {
      if (valueCase_ == 4) {
        return (Double) value_;
      }
      return 0D;
    }

    public static final int ARRAY_VALUE_FIELD_NUMBER = 5;

    /**
     * <code>.opamp.proto.ArrayValue array_value = 5;</code>
     *
     * @return Whether the arrayValue field is set.
     */
    @Override
    public boolean hasArrayValue() {
      return valueCase_ == 5;
    }

    /**
     * <code>.opamp.proto.ArrayValue array_value = 5;</code>
     *
     * @return The arrayValue.
     */
    @Override
    public ArrayValue getArrayValue() {
      if (valueCase_ == 5) {
        return (ArrayValue) value_;
      }
      return ArrayValue.getDefaultInstance();
    }

    /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
    @Override
    public ArrayValueOrBuilder getArrayValueOrBuilder() {
      if (valueCase_ == 5) {
        return (ArrayValue) value_;
      }
      return ArrayValue.getDefaultInstance();
    }

    public static final int KVLIST_VALUE_FIELD_NUMBER = 6;

    /**
     * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
     *
     * @return Whether the kvlistValue field is set.
     */
    @Override
    public boolean hasKvlistValue() {
      return valueCase_ == 6;
    }

    /**
     * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
     *
     * @return The kvlistValue.
     */
    @Override
    public KeyValueList getKvlistValue() {
      if (valueCase_ == 6) {
        return (KeyValueList) value_;
      }
      return KeyValueList.getDefaultInstance();
    }

    /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
    @Override
    public KeyValueListOrBuilder getKvlistValueOrBuilder() {
      if (valueCase_ == 6) {
        return (KeyValueList) value_;
      }
      return KeyValueList.getDefaultInstance();
    }

    public static final int BYTES_VALUE_FIELD_NUMBER = 7;

    /**
     * <code>bytes bytes_value = 7;</code>
     *
     * @return Whether the bytesValue field is set.
     */
    @Override
    public boolean hasBytesValue() {
      return valueCase_ == 7;
    }

    /**
     * <code>bytes bytes_value = 7;</code>
     *
     * @return The bytesValue.
     */
    @Override
    public com.google.protobuf.ByteString getBytesValue() {
      if (valueCase_ == 7) {
        return (com.google.protobuf.ByteString) value_;
      }
      return com.google.protobuf.ByteString.EMPTY;
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (valueCase_ == 1) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, value_);
      }
      if (valueCase_ == 2) {
        output.writeBool(2, (boolean) ((Boolean) value_));
      }
      if (valueCase_ == 3) {
        output.writeInt64(3, (long) ((Long) value_));
      }
      if (valueCase_ == 4) {
        output.writeDouble(4, (double) ((Double) value_));
      }
      if (valueCase_ == 5) {
        output.writeMessage(5, (ArrayValue) value_);
      }
      if (valueCase_ == 6) {
        output.writeMessage(6, (KeyValueList) value_);
      }
      if (valueCase_ == 7) {
        output.writeBytes(7, (com.google.protobuf.ByteString) value_);
      }
      getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (valueCase_ == 1) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, value_);
      }
      if (valueCase_ == 2) {
        size +=
            com.google.protobuf.CodedOutputStream.computeBoolSize(2, (boolean) ((Boolean) value_));
      }
      if (valueCase_ == 3) {
        size += com.google.protobuf.CodedOutputStream.computeInt64Size(3, (long) ((Long) value_));
      }
      if (valueCase_ == 4) {
        size +=
            com.google.protobuf.CodedOutputStream.computeDoubleSize(4, (double) ((Double) value_));
      }
      if (valueCase_ == 5) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(5, (ArrayValue) value_);
      }
      if (valueCase_ == 6) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(6, (KeyValueList) value_);
      }
      if (valueCase_ == 7) {
        size +=
            com.google.protobuf.CodedOutputStream.computeBytesSize(
                7, (com.google.protobuf.ByteString) value_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof AnyValue)) {
        return super.equals(obj);
      }
      AnyValue other = (AnyValue) obj;

      if (!getValueCase().equals(other.getValueCase())) return false;
      switch (valueCase_) {
        case 1:
          if (!getStringValue().equals(other.getStringValue())) return false;
          break;
        case 2:
          if (getBoolValue() != other.getBoolValue()) return false;
          break;
        case 3:
          if (getIntValue() != other.getIntValue()) return false;
          break;
        case 4:
          if (Double.doubleToLongBits(getDoubleValue())
              != Double.doubleToLongBits(other.getDoubleValue())) return false;
          break;
        case 5:
          if (!getArrayValue().equals(other.getArrayValue())) return false;
          break;
        case 6:
          if (!getKvlistValue().equals(other.getKvlistValue())) return false;
          break;
        case 7:
          if (!getBytesValue().equals(other.getBytesValue())) return false;
          break;
        case 0:
        default:
      }
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      switch (valueCase_) {
        case 1:
          hash = (37 * hash) + STRING_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + getStringValue().hashCode();
          break;
        case 2:
          hash = (37 * hash) + BOOL_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getBoolValue());
          break;
        case 3:
          hash = (37 * hash) + INT_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getIntValue());
          break;
        case 4:
          hash = (37 * hash) + DOUBLE_VALUE_FIELD_NUMBER;
          hash =
              (53 * hash)
                  + com.google.protobuf.Internal.hashLong(
                      Double.doubleToLongBits(getDoubleValue()));
          break;
        case 5:
          hash = (37 * hash) + ARRAY_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + getArrayValue().hashCode();
          break;
        case 6:
          hash = (37 * hash) + KVLIST_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + getKvlistValue().hashCode();
          break;
        case 7:
          hash = (37 * hash) + BYTES_VALUE_FIELD_NUMBER;
          hash = (53 * hash) + getBytesValue().hashCode();
          break;
        case 0:
        default:
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static AnyValue parseFrom(java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AnyValue parseFrom(
        java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AnyValue parseFrom(com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AnyValue parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AnyValue parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AnyValue parseFrom(
        byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AnyValue parseFrom(java.io.InputStream input) throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AnyValue parseFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static AnyValue parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static AnyValue parseDelimitedFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static AnyValue parseFrom(com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AnyValue parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(AnyValue prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     *
     *
     * <pre>
     * AnyValue is used to represent any type of attribute value. AnyValue may contain a
     * primitive value such as a string or integer or it may contain an arbitrary nested
     * object containing arrays, key-value lists and primitives.
     * </pre>
     *
     * Protobuf type {@code opamp.proto.AnyValue}
     */
    public static final class Builder
        extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
        implements
        // @@protoc_insertion_point(builder_implements:opamp.proto.AnyValue)
        AnyValueOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return Anyvalue.internal_static_opamp_proto_AnyValue_descriptor;
      }

      @Override
      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return Anyvalue.internal_static_opamp_proto_AnyValue_fieldAccessorTable
            .ensureFieldAccessorsInitialized(AnyValue.class, Builder.class);
      }

      // Construct using opamp.proto.Anyvalue.AnyValue.newBuilder()
      private Builder() {}

      private Builder(BuilderParent parent) {
        super(parent);
      }

      @Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        if (arrayValueBuilder_ != null) {
          arrayValueBuilder_.clear();
        }
        if (kvlistValueBuilder_ != null) {
          kvlistValueBuilder_.clear();
        }
        valueCase_ = 0;
        value_ = null;
        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return Anyvalue.internal_static_opamp_proto_AnyValue_descriptor;
      }

      @Override
      public AnyValue getDefaultInstanceForType() {
        return AnyValue.getDefaultInstance();
      }

      @Override
      public AnyValue build() {
        AnyValue result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public AnyValue buildPartial() {
        AnyValue result = new AnyValue(this);
        if (bitField0_ != 0) {
          buildPartial0(result);
        }
        buildPartialOneofs(result);
        onBuilt();
        return result;
      }

      private void buildPartial0(AnyValue result) {
        int from_bitField0_ = bitField0_;
      }

      private void buildPartialOneofs(AnyValue result) {
        result.valueCase_ = valueCase_;
        result.value_ = this.value_;
        if (valueCase_ == 5 && arrayValueBuilder_ != null) {
          result.value_ = arrayValueBuilder_.build();
        }
        if (valueCase_ == 6 && kvlistValueBuilder_ != null) {
          result.value_ = kvlistValueBuilder_.build();
        }
      }

      @Override
      public Builder clone() {
        return super.clone();
      }

      @Override
      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.setField(field, value);
      }

      @Override
      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @Override
      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof AnyValue) {
          return mergeFrom((AnyValue) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(AnyValue other) {
        if (other == AnyValue.getDefaultInstance()) return this;
        switch (other.getValueCase()) {
          case STRING_VALUE:
            {
              valueCase_ = 1;
              value_ = other.value_;
              onChanged();
              break;
            }
          case BOOL_VALUE:
            {
              setBoolValue(other.getBoolValue());
              break;
            }
          case INT_VALUE:
            {
              setIntValue(other.getIntValue());
              break;
            }
          case DOUBLE_VALUE:
            {
              setDoubleValue(other.getDoubleValue());
              break;
            }
          case ARRAY_VALUE:
            {
              mergeArrayValue(other.getArrayValue());
              break;
            }
          case KVLIST_VALUE:
            {
              mergeKvlistValue(other.getKvlistValue());
              break;
            }
          case BYTES_VALUE:
            {
              setBytesValue(other.getBytesValue());
              break;
            }
          case VALUE_NOT_SET:
            {
              break;
            }
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10:
                {
                  String s = input.readStringRequireUtf8();
                  valueCase_ = 1;
                  value_ = s;
                  break;
                } // case 10
              case 16:
                {
                  value_ = input.readBool();
                  valueCase_ = 2;
                  break;
                } // case 16
              case 24:
                {
                  value_ = input.readInt64();
                  valueCase_ = 3;
                  break;
                } // case 24
              case 33:
                {
                  value_ = input.readDouble();
                  valueCase_ = 4;
                  break;
                } // case 33
              case 42:
                {
                  input.readMessage(getArrayValueFieldBuilder().getBuilder(), extensionRegistry);
                  valueCase_ = 5;
                  break;
                } // case 42
              case 50:
                {
                  input.readMessage(getKvlistValueFieldBuilder().getBuilder(), extensionRegistry);
                  valueCase_ = 6;
                  break;
                } // case 50
              case 58:
                {
                  value_ = input.readBytes();
                  valueCase_ = 7;
                  break;
                } // case 58
              default:
                {
                  if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                    done = true; // was an endgroup tag
                  }
                  break;
                } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }

      private int valueCase_ = 0;
      private Object value_;

      public ValueCase getValueCase() {
        return ValueCase.forNumber(valueCase_);
      }

      public Builder clearValue() {
        valueCase_ = 0;
        value_ = null;
        onChanged();
        return this;
      }

      private int bitField0_;

      /**
       * <code>string string_value = 1;</code>
       *
       * @return Whether the stringValue field is set.
       */
      @Override
      public boolean hasStringValue() {
        return valueCase_ == 1;
      }

      /**
       * <code>string string_value = 1;</code>
       *
       * @return The stringValue.
       */
      @Override
      public String getStringValue() {
        Object ref = "";
        if (valueCase_ == 1) {
          ref = value_;
        }
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (valueCase_ == 1) {
            value_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>string string_value = 1;</code>
       *
       * @return The bytes for stringValue.
       */
      @Override
      public com.google.protobuf.ByteString getStringValueBytes() {
        Object ref = "";
        if (valueCase_ == 1) {
          ref = value_;
        }
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8((String) ref);
          if (valueCase_ == 1) {
            value_ = b;
          }
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string string_value = 1;</code>
       *
       * @param value The stringValue to set.
       * @return This builder for chaining.
       */
      public Builder setStringValue(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        valueCase_ = 1;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string string_value = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearStringValue() {
        if (valueCase_ == 1) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
        return this;
      }

      /**
       * <code>string string_value = 1;</code>
       *
       * @param value The bytes for stringValue to set.
       * @return This builder for chaining.
       */
      public Builder setStringValueBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);
        valueCase_ = 1;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>bool bool_value = 2;</code>
       *
       * @return Whether the boolValue field is set.
       */
      public boolean hasBoolValue() {
        return valueCase_ == 2;
      }

      /**
       * <code>bool bool_value = 2;</code>
       *
       * @return The boolValue.
       */
      public boolean getBoolValue() {
        if (valueCase_ == 2) {
          return (Boolean) value_;
        }
        return false;
      }

      /**
       * <code>bool bool_value = 2;</code>
       *
       * @param value The boolValue to set.
       * @return This builder for chaining.
       */
      public Builder setBoolValue(boolean value) {

        valueCase_ = 2;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>bool bool_value = 2;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearBoolValue() {
        if (valueCase_ == 2) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
        return this;
      }

      /**
       * <code>int64 int_value = 3;</code>
       *
       * @return Whether the intValue field is set.
       */
      public boolean hasIntValue() {
        return valueCase_ == 3;
      }

      /**
       * <code>int64 int_value = 3;</code>
       *
       * @return The intValue.
       */
      public long getIntValue() {
        if (valueCase_ == 3) {
          return (Long) value_;
        }
        return 0L;
      }

      /**
       * <code>int64 int_value = 3;</code>
       *
       * @param value The intValue to set.
       * @return This builder for chaining.
       */
      public Builder setIntValue(long value) {

        valueCase_ = 3;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int64 int_value = 3;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearIntValue() {
        if (valueCase_ == 3) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
        return this;
      }

      /**
       * <code>double double_value = 4;</code>
       *
       * @return Whether the doubleValue field is set.
       */
      public boolean hasDoubleValue() {
        return valueCase_ == 4;
      }

      /**
       * <code>double double_value = 4;</code>
       *
       * @return The doubleValue.
       */
      public double getDoubleValue() {
        if (valueCase_ == 4) {
          return (Double) value_;
        }
        return 0D;
      }

      /**
       * <code>double double_value = 4;</code>
       *
       * @param value The doubleValue to set.
       * @return This builder for chaining.
       */
      public Builder setDoubleValue(double value) {

        valueCase_ = 4;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>double double_value = 4;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearDoubleValue() {
        if (valueCase_ == 4) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
        return this;
      }

      private com.google.protobuf.SingleFieldBuilderV3<
              ArrayValue, ArrayValue.Builder, ArrayValueOrBuilder>
          arrayValueBuilder_;

      /**
       * <code>.opamp.proto.ArrayValue array_value = 5;</code>
       *
       * @return Whether the arrayValue field is set.
       */
      @Override
      public boolean hasArrayValue() {
        return valueCase_ == 5;
      }

      /**
       * <code>.opamp.proto.ArrayValue array_value = 5;</code>
       *
       * @return The arrayValue.
       */
      @Override
      public ArrayValue getArrayValue() {
        if (arrayValueBuilder_ == null) {
          if (valueCase_ == 5) {
            return (ArrayValue) value_;
          }
          return ArrayValue.getDefaultInstance();
        } else {
          if (valueCase_ == 5) {
            return arrayValueBuilder_.getMessage();
          }
          return ArrayValue.getDefaultInstance();
        }
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      public Builder setArrayValue(ArrayValue value) {
        if (arrayValueBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          value_ = value;
          onChanged();
        } else {
          arrayValueBuilder_.setMessage(value);
        }
        valueCase_ = 5;
        return this;
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      public Builder setArrayValue(ArrayValue.Builder builderForValue) {
        if (arrayValueBuilder_ == null) {
          value_ = builderForValue.build();
          onChanged();
        } else {
          arrayValueBuilder_.setMessage(builderForValue.build());
        }
        valueCase_ = 5;
        return this;
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      public Builder mergeArrayValue(ArrayValue value) {
        if (arrayValueBuilder_ == null) {
          if (valueCase_ == 5 && value_ != ArrayValue.getDefaultInstance()) {
            value_ = ArrayValue.newBuilder((ArrayValue) value_).mergeFrom(value).buildPartial();
          } else {
            value_ = value;
          }
          onChanged();
        } else {
          if (valueCase_ == 5) {
            arrayValueBuilder_.mergeFrom(value);
          } else {
            arrayValueBuilder_.setMessage(value);
          }
        }
        valueCase_ = 5;
        return this;
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      public Builder clearArrayValue() {
        if (arrayValueBuilder_ == null) {
          if (valueCase_ == 5) {
            valueCase_ = 0;
            value_ = null;
            onChanged();
          }
        } else {
          if (valueCase_ == 5) {
            valueCase_ = 0;
            value_ = null;
          }
          arrayValueBuilder_.clear();
        }
        return this;
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      public ArrayValue.Builder getArrayValueBuilder() {
        return getArrayValueFieldBuilder().getBuilder();
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      @Override
      public ArrayValueOrBuilder getArrayValueOrBuilder() {
        if ((valueCase_ == 5) && (arrayValueBuilder_ != null)) {
          return arrayValueBuilder_.getMessageOrBuilder();
        } else {
          if (valueCase_ == 5) {
            return (ArrayValue) value_;
          }
          return ArrayValue.getDefaultInstance();
        }
      }

      /** <code>.opamp.proto.ArrayValue array_value = 5;</code> */
      private com.google.protobuf.SingleFieldBuilderV3<
              ArrayValue, ArrayValue.Builder, ArrayValueOrBuilder>
          getArrayValueFieldBuilder() {
        if (arrayValueBuilder_ == null) {
          if (!(valueCase_ == 5)) {
            value_ = ArrayValue.getDefaultInstance();
          }
          arrayValueBuilder_ =
              new com.google.protobuf.SingleFieldBuilderV3<
                  ArrayValue, ArrayValue.Builder, ArrayValueOrBuilder>(
                  (ArrayValue) value_, getParentForChildren(), isClean());
          value_ = null;
        }
        valueCase_ = 5;
        onChanged();
        return arrayValueBuilder_;
      }

      private com.google.protobuf.SingleFieldBuilderV3<
              KeyValueList, KeyValueList.Builder, KeyValueListOrBuilder>
          kvlistValueBuilder_;

      /**
       * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
       *
       * @return Whether the kvlistValue field is set.
       */
      @Override
      public boolean hasKvlistValue() {
        return valueCase_ == 6;
      }

      /**
       * <code>.opamp.proto.KeyValueList kvlist_value = 6;</code>
       *
       * @return The kvlistValue.
       */
      @Override
      public KeyValueList getKvlistValue() {
        if (kvlistValueBuilder_ == null) {
          if (valueCase_ == 6) {
            return (KeyValueList) value_;
          }
          return KeyValueList.getDefaultInstance();
        } else {
          if (valueCase_ == 6) {
            return kvlistValueBuilder_.getMessage();
          }
          return KeyValueList.getDefaultInstance();
        }
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      public Builder setKvlistValue(KeyValueList value) {
        if (kvlistValueBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          value_ = value;
          onChanged();
        } else {
          kvlistValueBuilder_.setMessage(value);
        }
        valueCase_ = 6;
        return this;
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      public Builder setKvlistValue(KeyValueList.Builder builderForValue) {
        if (kvlistValueBuilder_ == null) {
          value_ = builderForValue.build();
          onChanged();
        } else {
          kvlistValueBuilder_.setMessage(builderForValue.build());
        }
        valueCase_ = 6;
        return this;
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      public Builder mergeKvlistValue(KeyValueList value) {
        if (kvlistValueBuilder_ == null) {
          if (valueCase_ == 6 && value_ != KeyValueList.getDefaultInstance()) {
            value_ = KeyValueList.newBuilder((KeyValueList) value_).mergeFrom(value).buildPartial();
          } else {
            value_ = value;
          }
          onChanged();
        } else {
          if (valueCase_ == 6) {
            kvlistValueBuilder_.mergeFrom(value);
          } else {
            kvlistValueBuilder_.setMessage(value);
          }
        }
        valueCase_ = 6;
        return this;
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      public Builder clearKvlistValue() {
        if (kvlistValueBuilder_ == null) {
          if (valueCase_ == 6) {
            valueCase_ = 0;
            value_ = null;
            onChanged();
          }
        } else {
          if (valueCase_ == 6) {
            valueCase_ = 0;
            value_ = null;
          }
          kvlistValueBuilder_.clear();
        }
        return this;
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      public KeyValueList.Builder getKvlistValueBuilder() {
        return getKvlistValueFieldBuilder().getBuilder();
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      @Override
      public KeyValueListOrBuilder getKvlistValueOrBuilder() {
        if ((valueCase_ == 6) && (kvlistValueBuilder_ != null)) {
          return kvlistValueBuilder_.getMessageOrBuilder();
        } else {
          if (valueCase_ == 6) {
            return (KeyValueList) value_;
          }
          return KeyValueList.getDefaultInstance();
        }
      }

      /** <code>.opamp.proto.KeyValueList kvlist_value = 6;</code> */
      private com.google.protobuf.SingleFieldBuilderV3<
              KeyValueList, KeyValueList.Builder, KeyValueListOrBuilder>
          getKvlistValueFieldBuilder() {
        if (kvlistValueBuilder_ == null) {
          if (!(valueCase_ == 6)) {
            value_ = KeyValueList.getDefaultInstance();
          }
          kvlistValueBuilder_ =
              new com.google.protobuf.SingleFieldBuilderV3<
                  KeyValueList, KeyValueList.Builder, KeyValueListOrBuilder>(
                  (KeyValueList) value_, getParentForChildren(), isClean());
          value_ = null;
        }
        valueCase_ = 6;
        onChanged();
        return kvlistValueBuilder_;
      }

      /**
       * <code>bytes bytes_value = 7;</code>
       *
       * @return Whether the bytesValue field is set.
       */
      public boolean hasBytesValue() {
        return valueCase_ == 7;
      }

      /**
       * <code>bytes bytes_value = 7;</code>
       *
       * @return The bytesValue.
       */
      public com.google.protobuf.ByteString getBytesValue() {
        if (valueCase_ == 7) {
          return (com.google.protobuf.ByteString) value_;
        }
        return com.google.protobuf.ByteString.EMPTY;
      }

      /**
       * <code>bytes bytes_value = 7;</code>
       *
       * @param value The bytesValue to set.
       * @return This builder for chaining.
       */
      public Builder setBytesValue(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        valueCase_ = 7;
        value_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>bytes bytes_value = 7;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearBytesValue() {
        if (valueCase_ == 7) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
        return this;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:opamp.proto.AnyValue)
    }

    // @@protoc_insertion_point(class_scope:opamp.proto.AnyValue)
    private static final AnyValue DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new AnyValue();
    }

    public static AnyValue getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<AnyValue> PARSER =
        new com.google.protobuf.AbstractParser<AnyValue>() {
          @Override
          public AnyValue parsePartialFrom(
              com.google.protobuf.CodedInputStream input,
              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
              builder.mergeFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
              throw e.setUnfinishedMessage(builder.buildPartial());
            } catch (com.google.protobuf.UninitializedMessageException e) {
              throw e.asInvalidProtocolBufferException()
                  .setUnfinishedMessage(builder.buildPartial());
            } catch (java.io.IOException e) {
              throw new com.google.protobuf.InvalidProtocolBufferException(e)
                  .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
          }
        };

    public static com.google.protobuf.Parser<AnyValue> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<AnyValue> getParserForType() {
      return PARSER;
    }

    @Override
    public AnyValue getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }
  }

  public interface ArrayValueOrBuilder
      extends
      // @@protoc_insertion_point(interface_extends:opamp.proto.ArrayValue)
      com.google.protobuf.MessageOrBuilder {

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    java.util.List<AnyValue> getValuesList();

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    AnyValue getValues(int index);

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    int getValuesCount();

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    java.util.List<? extends AnyValueOrBuilder> getValuesOrBuilderList();

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    AnyValueOrBuilder getValuesOrBuilder(int index);
  }

  /**
   *
   *
   * <pre>
   * ArrayValue is a list of AnyValue messages. We need ArrayValue as a message
   * since oneof in AnyValue does not allow repeated fields.
   * </pre>
   *
   * Protobuf type {@code opamp.proto.ArrayValue}
   */
  public static final class ArrayValue extends com.google.protobuf.GeneratedMessageV3
      implements
      // @@protoc_insertion_point(message_implements:opamp.proto.ArrayValue)
      ArrayValueOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ArrayValue.newBuilder() to construct.
    private ArrayValue(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private ArrayValue() {
      values_ = java.util.Collections.emptyList();
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
      return new ArrayValue();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return Anyvalue.internal_static_opamp_proto_ArrayValue_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return Anyvalue.internal_static_opamp_proto_ArrayValue_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ArrayValue.class, Builder.class);
    }

    public static final int VALUES_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private java.util.List<AnyValue> values_;

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    @Override
    public java.util.List<AnyValue> getValuesList() {
      return values_;
    }

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    @Override
    public java.util.List<? extends AnyValueOrBuilder> getValuesOrBuilderList() {
      return values_;
    }

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    @Override
    public int getValuesCount() {
      return values_.size();
    }

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    @Override
    public AnyValue getValues(int index) {
      return values_.get(index);
    }

    /**
     *
     *
     * <pre>
     * Array of values. The array may be empty (contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.AnyValue values = 1;</code>
     */
    @Override
    public AnyValueOrBuilder getValuesOrBuilder(int index) {
      return values_.get(index);
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < values_.size(); i++) {
        output.writeMessage(1, values_.get(i));
      }
      getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      for (int i = 0; i < values_.size(); i++) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, values_.get(i));
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ArrayValue)) {
        return super.equals(obj);
      }
      ArrayValue other = (ArrayValue) obj;

      if (!getValuesList().equals(other.getValuesList())) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getValuesCount() > 0) {
        hash = (37 * hash) + VALUES_FIELD_NUMBER;
        hash = (53 * hash) + getValuesList().hashCode();
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static ArrayValue parseFrom(java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ArrayValue parseFrom(
        java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ArrayValue parseFrom(com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ArrayValue parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ArrayValue parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ArrayValue parseFrom(
        byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ArrayValue parseFrom(java.io.InputStream input) throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ArrayValue parseFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static ArrayValue parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ArrayValue parseDelimitedFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static ArrayValue parseFrom(com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ArrayValue parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ArrayValue prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     *
     *
     * <pre>
     * ArrayValue is a list of AnyValue messages. We need ArrayValue as a message
     * since oneof in AnyValue does not allow repeated fields.
     * </pre>
     *
     * Protobuf type {@code opamp.proto.ArrayValue}
     */
    public static final class Builder
        extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
        implements
        // @@protoc_insertion_point(builder_implements:opamp.proto.ArrayValue)
        ArrayValueOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return Anyvalue.internal_static_opamp_proto_ArrayValue_descriptor;
      }

      @Override
      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return Anyvalue.internal_static_opamp_proto_ArrayValue_fieldAccessorTable
            .ensureFieldAccessorsInitialized(ArrayValue.class, Builder.class);
      }

      // Construct using opamp.proto.Anyvalue.ArrayValue.newBuilder()
      private Builder() {}

      private Builder(BuilderParent parent) {
        super(parent);
      }

      @Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        if (valuesBuilder_ == null) {
          values_ = java.util.Collections.emptyList();
        } else {
          values_ = null;
          valuesBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return Anyvalue.internal_static_opamp_proto_ArrayValue_descriptor;
      }

      @Override
      public ArrayValue getDefaultInstanceForType() {
        return ArrayValue.getDefaultInstance();
      }

      @Override
      public ArrayValue build() {
        ArrayValue result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public ArrayValue buildPartial() {
        ArrayValue result = new ArrayValue(this);
        buildPartialRepeatedFields(result);
        if (bitField0_ != 0) {
          buildPartial0(result);
        }
        onBuilt();
        return result;
      }

      private void buildPartialRepeatedFields(ArrayValue result) {
        if (valuesBuilder_ == null) {
          if (((bitField0_ & 0x00000001) != 0)) {
            values_ = java.util.Collections.unmodifiableList(values_);
            bitField0_ = (bitField0_ & ~0x00000001);
          }
          result.values_ = values_;
        } else {
          result.values_ = valuesBuilder_.build();
        }
      }

      private void buildPartial0(ArrayValue result) {
        int from_bitField0_ = bitField0_;
      }

      @Override
      public Builder clone() {
        return super.clone();
      }

      @Override
      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.setField(field, value);
      }

      @Override
      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @Override
      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ArrayValue) {
          return mergeFrom((ArrayValue) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ArrayValue other) {
        if (other == ArrayValue.getDefaultInstance()) return this;
        if (valuesBuilder_ == null) {
          if (!other.values_.isEmpty()) {
            if (values_.isEmpty()) {
              values_ = other.values_;
              bitField0_ = (bitField0_ & ~0x00000001);
            } else {
              ensureValuesIsMutable();
              values_.addAll(other.values_);
            }
            onChanged();
          }
        } else {
          if (!other.values_.isEmpty()) {
            if (valuesBuilder_.isEmpty()) {
              valuesBuilder_.dispose();
              valuesBuilder_ = null;
              values_ = other.values_;
              bitField0_ = (bitField0_ & ~0x00000001);
              valuesBuilder_ =
                  com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                      ? getValuesFieldBuilder()
                      : null;
            } else {
              valuesBuilder_.addAllMessages(other.values_);
            }
          }
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10:
                {
                  AnyValue m = input.readMessage(AnyValue.parser(), extensionRegistry);
                  if (valuesBuilder_ == null) {
                    ensureValuesIsMutable();
                    values_.add(m);
                  } else {
                    valuesBuilder_.addMessage(m);
                  }
                  break;
                } // case 10
              default:
                {
                  if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                    done = true; // was an endgroup tag
                  }
                  break;
                } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }

      private int bitField0_;

      private java.util.List<AnyValue> values_ = java.util.Collections.emptyList();

      private void ensureValuesIsMutable() {
        if (!((bitField0_ & 0x00000001) != 0)) {
          values_ = new java.util.ArrayList<AnyValue>(values_);
          bitField0_ |= 0x00000001;
        }
      }

      private com.google.protobuf.RepeatedFieldBuilderV3<
              AnyValue, AnyValue.Builder, AnyValueOrBuilder>
          valuesBuilder_;

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public java.util.List<AnyValue> getValuesList() {
        if (valuesBuilder_ == null) {
          return java.util.Collections.unmodifiableList(values_);
        } else {
          return valuesBuilder_.getMessageList();
        }
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public int getValuesCount() {
        if (valuesBuilder_ == null) {
          return values_.size();
        } else {
          return valuesBuilder_.getCount();
        }
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public AnyValue getValues(int index) {
        if (valuesBuilder_ == null) {
          return values_.get(index);
        } else {
          return valuesBuilder_.getMessage(index);
        }
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder setValues(int index, AnyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.set(index, value);
          onChanged();
        } else {
          valuesBuilder_.setMessage(index, value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder setValues(int index, AnyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.set(index, builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder addValues(AnyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.add(value);
          onChanged();
        } else {
          valuesBuilder_.addMessage(value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder addValues(int index, AnyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.add(index, value);
          onChanged();
        } else {
          valuesBuilder_.addMessage(index, value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder addValues(AnyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.add(builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder addValues(int index, AnyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.add(index, builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder addAllValues(Iterable<? extends AnyValue> values) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(values, values_);
          onChanged();
        } else {
          valuesBuilder_.addAllMessages(values);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder clearValues() {
        if (valuesBuilder_ == null) {
          values_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
          onChanged();
        } else {
          valuesBuilder_.clear();
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public Builder removeValues(int index) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.remove(index);
          onChanged();
        } else {
          valuesBuilder_.remove(index);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public AnyValue.Builder getValuesBuilder(int index) {
        return getValuesFieldBuilder().getBuilder(index);
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public AnyValueOrBuilder getValuesOrBuilder(int index) {
        if (valuesBuilder_ == null) {
          return values_.get(index);
        } else {
          return valuesBuilder_.getMessageOrBuilder(index);
        }
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public java.util.List<? extends AnyValueOrBuilder> getValuesOrBuilderList() {
        if (valuesBuilder_ != null) {
          return valuesBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(values_);
        }
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public AnyValue.Builder addValuesBuilder() {
        return getValuesFieldBuilder().addBuilder(AnyValue.getDefaultInstance());
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public AnyValue.Builder addValuesBuilder(int index) {
        return getValuesFieldBuilder().addBuilder(index, AnyValue.getDefaultInstance());
      }

      /**
       *
       *
       * <pre>
       * Array of values. The array may be empty (contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.AnyValue values = 1;</code>
       */
      public java.util.List<AnyValue.Builder> getValuesBuilderList() {
        return getValuesFieldBuilder().getBuilderList();
      }

      private com.google.protobuf.RepeatedFieldBuilderV3<
              AnyValue, AnyValue.Builder, AnyValueOrBuilder>
          getValuesFieldBuilder() {
        if (valuesBuilder_ == null) {
          valuesBuilder_ =
              new com.google.protobuf.RepeatedFieldBuilderV3<
                  AnyValue, AnyValue.Builder, AnyValueOrBuilder>(
                  values_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
          values_ = null;
        }
        return valuesBuilder_;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:opamp.proto.ArrayValue)
    }

    // @@protoc_insertion_point(class_scope:opamp.proto.ArrayValue)
    private static final ArrayValue DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new ArrayValue();
    }

    public static ArrayValue getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<ArrayValue> PARSER =
        new com.google.protobuf.AbstractParser<ArrayValue>() {
          @Override
          public ArrayValue parsePartialFrom(
              com.google.protobuf.CodedInputStream input,
              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
              builder.mergeFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
              throw e.setUnfinishedMessage(builder.buildPartial());
            } catch (com.google.protobuf.UninitializedMessageException e) {
              throw e.asInvalidProtocolBufferException()
                  .setUnfinishedMessage(builder.buildPartial());
            } catch (java.io.IOException e) {
              throw new com.google.protobuf.InvalidProtocolBufferException(e)
                  .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
          }
        };

    public static com.google.protobuf.Parser<ArrayValue> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ArrayValue> getParserForType() {
      return PARSER;
    }

    @Override
    public ArrayValue getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }
  }

  public interface KeyValueListOrBuilder
      extends
      // @@protoc_insertion_point(interface_extends:opamp.proto.KeyValueList)
      com.google.protobuf.MessageOrBuilder {

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    java.util.List<KeyValue> getValuesList();

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    KeyValue getValues(int index);

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    int getValuesCount();

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    java.util.List<? extends KeyValueOrBuilder> getValuesOrBuilderList();

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    KeyValueOrBuilder getValuesOrBuilder(int index);
  }

  /**
   *
   *
   * <pre>
   * KeyValueList is a list of KeyValue messages. We need KeyValueList as a message
   * since `oneof` in AnyValue does not allow repeated fields. Everywhere else where we need
   * a list of KeyValue messages (e.g. in Span) we use `repeated KeyValue` directly to
   * avoid unnecessary extra wrapping (which slows down the protocol). The 2 approaches
   * are semantically equivalent.
   * </pre>
   *
   * Protobuf type {@code opamp.proto.KeyValueList}
   */
  public static final class KeyValueList extends com.google.protobuf.GeneratedMessageV3
      implements
      // @@protoc_insertion_point(message_implements:opamp.proto.KeyValueList)
      KeyValueListOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use KeyValueList.newBuilder() to construct.
    private KeyValueList(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private KeyValueList() {
      values_ = java.util.Collections.emptyList();
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
      return new KeyValueList();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return Anyvalue.internal_static_opamp_proto_KeyValueList_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return Anyvalue.internal_static_opamp_proto_KeyValueList_fieldAccessorTable
          .ensureFieldAccessorsInitialized(KeyValueList.class, Builder.class);
    }

    public static final int VALUES_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private java.util.List<KeyValue> values_;

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    @Override
    public java.util.List<KeyValue> getValuesList() {
      return values_;
    }

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    @Override
    public java.util.List<? extends KeyValueOrBuilder> getValuesOrBuilderList() {
      return values_;
    }

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    @Override
    public int getValuesCount() {
      return values_.size();
    }

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    @Override
    public KeyValue getValues(int index) {
      return values_.get(index);
    }

    /**
     *
     *
     * <pre>
     * A collection of key/value pairs of key-value pairs. The list may be empty (may
     * contain 0 elements).
     * </pre>
     *
     * <code>repeated .opamp.proto.KeyValue values = 1;</code>
     */
    @Override
    public KeyValueOrBuilder getValuesOrBuilder(int index) {
      return values_.get(index);
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < values_.size(); i++) {
        output.writeMessage(1, values_.get(i));
      }
      getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      for (int i = 0; i < values_.size(); i++) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, values_.get(i));
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof KeyValueList)) {
        return super.equals(obj);
      }
      KeyValueList other = (KeyValueList) obj;

      if (!getValuesList().equals(other.getValuesList())) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getValuesCount() > 0) {
        hash = (37 * hash) + VALUES_FIELD_NUMBER;
        hash = (53 * hash) + getValuesList().hashCode();
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static KeyValueList parseFrom(java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValueList parseFrom(
        java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValueList parseFrom(com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValueList parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValueList parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValueList parseFrom(
        byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValueList parseFrom(java.io.InputStream input) throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static KeyValueList parseFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static KeyValueList parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static KeyValueList parseDelimitedFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static KeyValueList parseFrom(com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static KeyValueList parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(KeyValueList prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     *
     *
     * <pre>
     * KeyValueList is a list of KeyValue messages. We need KeyValueList as a message
     * since `oneof` in AnyValue does not allow repeated fields. Everywhere else where we need
     * a list of KeyValue messages (e.g. in Span) we use `repeated KeyValue` directly to
     * avoid unnecessary extra wrapping (which slows down the protocol). The 2 approaches
     * are semantically equivalent.
     * </pre>
     *
     * Protobuf type {@code opamp.proto.KeyValueList}
     */
    public static final class Builder
        extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
        implements
        // @@protoc_insertion_point(builder_implements:opamp.proto.KeyValueList)
        KeyValueListOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return Anyvalue.internal_static_opamp_proto_KeyValueList_descriptor;
      }

      @Override
      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return Anyvalue.internal_static_opamp_proto_KeyValueList_fieldAccessorTable
            .ensureFieldAccessorsInitialized(KeyValueList.class, Builder.class);
      }

      // Construct using opamp.proto.Anyvalue.KeyValueList.newBuilder()
      private Builder() {}

      private Builder(BuilderParent parent) {
        super(parent);
      }

      @Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        if (valuesBuilder_ == null) {
          values_ = java.util.Collections.emptyList();
        } else {
          values_ = null;
          valuesBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return Anyvalue.internal_static_opamp_proto_KeyValueList_descriptor;
      }

      @Override
      public KeyValueList getDefaultInstanceForType() {
        return KeyValueList.getDefaultInstance();
      }

      @Override
      public KeyValueList build() {
        KeyValueList result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public KeyValueList buildPartial() {
        KeyValueList result = new KeyValueList(this);
        buildPartialRepeatedFields(result);
        if (bitField0_ != 0) {
          buildPartial0(result);
        }
        onBuilt();
        return result;
      }

      private void buildPartialRepeatedFields(KeyValueList result) {
        if (valuesBuilder_ == null) {
          if (((bitField0_ & 0x00000001) != 0)) {
            values_ = java.util.Collections.unmodifiableList(values_);
            bitField0_ = (bitField0_ & ~0x00000001);
          }
          result.values_ = values_;
        } else {
          result.values_ = valuesBuilder_.build();
        }
      }

      private void buildPartial0(KeyValueList result) {
        int from_bitField0_ = bitField0_;
      }

      @Override
      public Builder clone() {
        return super.clone();
      }

      @Override
      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.setField(field, value);
      }

      @Override
      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @Override
      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof KeyValueList) {
          return mergeFrom((KeyValueList) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(KeyValueList other) {
        if (other == KeyValueList.getDefaultInstance()) return this;
        if (valuesBuilder_ == null) {
          if (!other.values_.isEmpty()) {
            if (values_.isEmpty()) {
              values_ = other.values_;
              bitField0_ = (bitField0_ & ~0x00000001);
            } else {
              ensureValuesIsMutable();
              values_.addAll(other.values_);
            }
            onChanged();
          }
        } else {
          if (!other.values_.isEmpty()) {
            if (valuesBuilder_.isEmpty()) {
              valuesBuilder_.dispose();
              valuesBuilder_ = null;
              values_ = other.values_;
              bitField0_ = (bitField0_ & ~0x00000001);
              valuesBuilder_ =
                  com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                      ? getValuesFieldBuilder()
                      : null;
            } else {
              valuesBuilder_.addAllMessages(other.values_);
            }
          }
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10:
                {
                  KeyValue m = input.readMessage(KeyValue.parser(), extensionRegistry);
                  if (valuesBuilder_ == null) {
                    ensureValuesIsMutable();
                    values_.add(m);
                  } else {
                    valuesBuilder_.addMessage(m);
                  }
                  break;
                } // case 10
              default:
                {
                  if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                    done = true; // was an endgroup tag
                  }
                  break;
                } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }

      private int bitField0_;

      private java.util.List<KeyValue> values_ = java.util.Collections.emptyList();

      private void ensureValuesIsMutable() {
        if (!((bitField0_ & 0x00000001) != 0)) {
          values_ = new java.util.ArrayList<KeyValue>(values_);
          bitField0_ |= 0x00000001;
        }
      }

      private com.google.protobuf.RepeatedFieldBuilderV3<
              KeyValue, KeyValue.Builder, KeyValueOrBuilder>
          valuesBuilder_;

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public java.util.List<KeyValue> getValuesList() {
        if (valuesBuilder_ == null) {
          return java.util.Collections.unmodifiableList(values_);
        } else {
          return valuesBuilder_.getMessageList();
        }
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public int getValuesCount() {
        if (valuesBuilder_ == null) {
          return values_.size();
        } else {
          return valuesBuilder_.getCount();
        }
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public KeyValue getValues(int index) {
        if (valuesBuilder_ == null) {
          return values_.get(index);
        } else {
          return valuesBuilder_.getMessage(index);
        }
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder setValues(int index, KeyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.set(index, value);
          onChanged();
        } else {
          valuesBuilder_.setMessage(index, value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder setValues(int index, KeyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.set(index, builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder addValues(KeyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.add(value);
          onChanged();
        } else {
          valuesBuilder_.addMessage(value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder addValues(int index, KeyValue value) {
        if (valuesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureValuesIsMutable();
          values_.add(index, value);
          onChanged();
        } else {
          valuesBuilder_.addMessage(index, value);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder addValues(KeyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.add(builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder addValues(int index, KeyValue.Builder builderForValue) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.add(index, builderForValue.build());
          onChanged();
        } else {
          valuesBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder addAllValues(Iterable<? extends KeyValue> values) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(values, values_);
          onChanged();
        } else {
          valuesBuilder_.addAllMessages(values);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder clearValues() {
        if (valuesBuilder_ == null) {
          values_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
          onChanged();
        } else {
          valuesBuilder_.clear();
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public Builder removeValues(int index) {
        if (valuesBuilder_ == null) {
          ensureValuesIsMutable();
          values_.remove(index);
          onChanged();
        } else {
          valuesBuilder_.remove(index);
        }
        return this;
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public KeyValue.Builder getValuesBuilder(int index) {
        return getValuesFieldBuilder().getBuilder(index);
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public KeyValueOrBuilder getValuesOrBuilder(int index) {
        if (valuesBuilder_ == null) {
          return values_.get(index);
        } else {
          return valuesBuilder_.getMessageOrBuilder(index);
        }
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public java.util.List<? extends KeyValueOrBuilder> getValuesOrBuilderList() {
        if (valuesBuilder_ != null) {
          return valuesBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(values_);
        }
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public KeyValue.Builder addValuesBuilder() {
        return getValuesFieldBuilder().addBuilder(KeyValue.getDefaultInstance());
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public KeyValue.Builder addValuesBuilder(int index) {
        return getValuesFieldBuilder().addBuilder(index, KeyValue.getDefaultInstance());
      }

      /**
       *
       *
       * <pre>
       * A collection of key/value pairs of key-value pairs. The list may be empty (may
       * contain 0 elements).
       * </pre>
       *
       * <code>repeated .opamp.proto.KeyValue values = 1;</code>
       */
      public java.util.List<KeyValue.Builder> getValuesBuilderList() {
        return getValuesFieldBuilder().getBuilderList();
      }

      private com.google.protobuf.RepeatedFieldBuilderV3<
              KeyValue, KeyValue.Builder, KeyValueOrBuilder>
          getValuesFieldBuilder() {
        if (valuesBuilder_ == null) {
          valuesBuilder_ =
              new com.google.protobuf.RepeatedFieldBuilderV3<
                  KeyValue, KeyValue.Builder, KeyValueOrBuilder>(
                  values_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
          values_ = null;
        }
        return valuesBuilder_;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:opamp.proto.KeyValueList)
    }

    // @@protoc_insertion_point(class_scope:opamp.proto.KeyValueList)
    private static final KeyValueList DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new KeyValueList();
    }

    public static KeyValueList getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KeyValueList> PARSER =
        new com.google.protobuf.AbstractParser<KeyValueList>() {
          @Override
          public KeyValueList parsePartialFrom(
              com.google.protobuf.CodedInputStream input,
              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
              builder.mergeFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
              throw e.setUnfinishedMessage(builder.buildPartial());
            } catch (com.google.protobuf.UninitializedMessageException e) {
              throw e.asInvalidProtocolBufferException()
                  .setUnfinishedMessage(builder.buildPartial());
            } catch (java.io.IOException e) {
              throw new com.google.protobuf.InvalidProtocolBufferException(e)
                  .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
          }
        };

    public static com.google.protobuf.Parser<KeyValueList> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<KeyValueList> getParserForType() {
      return PARSER;
    }

    @Override
    public KeyValueList getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }
  }

  public interface KeyValueOrBuilder
      extends
      // @@protoc_insertion_point(interface_extends:opamp.proto.KeyValue)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string key = 1;</code>
     *
     * @return The key.
     */
    String getKey();

    /**
     * <code>string key = 1;</code>
     *
     * @return The bytes for key.
     */
    com.google.protobuf.ByteString getKeyBytes();

    /**
     * <code>.opamp.proto.AnyValue value = 2;</code>
     *
     * @return Whether the value field is set.
     */
    boolean hasValue();

    /**
     * <code>.opamp.proto.AnyValue value = 2;</code>
     *
     * @return The value.
     */
    AnyValue getValue();

    /** <code>.opamp.proto.AnyValue value = 2;</code> */
    AnyValueOrBuilder getValueOrBuilder();
  }

  /**
   *
   *
   * <pre>
   * KeyValue is a key-value pair that is used to store Span attributes, Link
   * attributes, etc.
   * </pre>
   *
   * Protobuf type {@code opamp.proto.KeyValue}
   */
  public static final class KeyValue extends com.google.protobuf.GeneratedMessageV3
      implements
      // @@protoc_insertion_point(message_implements:opamp.proto.KeyValue)
      KeyValueOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use KeyValue.newBuilder() to construct.
    private KeyValue(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private KeyValue() {
      key_ = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
      return new KeyValue();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return Anyvalue.internal_static_opamp_proto_KeyValue_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return Anyvalue.internal_static_opamp_proto_KeyValue_fieldAccessorTable
          .ensureFieldAccessorsInitialized(KeyValue.class, Builder.class);
    }

    private int bitField0_;
    public static final int KEY_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object key_ = "";

    /**
     * <code>string key = 1;</code>
     *
     * @return The key.
     */
    @Override
    public String getKey() {
      Object ref = key_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        key_ = s;
        return s;
      }
    }

    /**
     * <code>string key = 1;</code>
     *
     * @return The bytes for key.
     */
    @Override
    public com.google.protobuf.ByteString getKeyBytes() {
      Object ref = key_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        key_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int VALUE_FIELD_NUMBER = 2;
    private AnyValue value_;

    /**
     * <code>.opamp.proto.AnyValue value = 2;</code>
     *
     * @return Whether the value field is set.
     */
    @Override
    public boolean hasValue() {
      return ((bitField0_ & 0x00000001) != 0);
    }

    /**
     * <code>.opamp.proto.AnyValue value = 2;</code>
     *
     * @return The value.
     */
    @Override
    public AnyValue getValue() {
      return value_ == null ? AnyValue.getDefaultInstance() : value_;
    }

    /** <code>.opamp.proto.AnyValue value = 2;</code> */
    @Override
    public AnyValueOrBuilder getValueOrBuilder() {
      return value_ == null ? AnyValue.getDefaultInstance() : value_;
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(key_)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, key_);
      }
      if (((bitField0_ & 0x00000001) != 0)) {
        output.writeMessage(2, getValue());
      }
      getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(key_)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, key_);
      }
      if (((bitField0_ & 0x00000001) != 0)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getValue());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof KeyValue)) {
        return super.equals(obj);
      }
      KeyValue other = (KeyValue) obj;

      if (!getKey().equals(other.getKey())) return false;
      if (hasValue() != other.hasValue()) return false;
      if (hasValue()) {
        if (!getValue().equals(other.getValue())) return false;
      }
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + KEY_FIELD_NUMBER;
      hash = (53 * hash) + getKey().hashCode();
      if (hasValue()) {
        hash = (37 * hash) + VALUE_FIELD_NUMBER;
        hash = (53 * hash) + getValue().hashCode();
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static KeyValue parseFrom(java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValue parseFrom(
        java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValue parseFrom(com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValue parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValue parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static KeyValue parseFrom(
        byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static KeyValue parseFrom(java.io.InputStream input) throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static KeyValue parseFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static KeyValue parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static KeyValue parseDelimitedFrom(
        java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
          PARSER, input, extensionRegistry);
    }

    public static KeyValue parseFrom(com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static KeyValue parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
          PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(KeyValue prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     *
     *
     * <pre>
     * KeyValue is a key-value pair that is used to store Span attributes, Link
     * attributes, etc.
     * </pre>
     *
     * Protobuf type {@code opamp.proto.KeyValue}
     */
    public static final class Builder
        extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
        implements
        // @@protoc_insertion_point(builder_implements:opamp.proto.KeyValue)
        KeyValueOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return Anyvalue.internal_static_opamp_proto_KeyValue_descriptor;
      }

      @Override
      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return Anyvalue.internal_static_opamp_proto_KeyValue_fieldAccessorTable
            .ensureFieldAccessorsInitialized(KeyValue.class, Builder.class);
      }

      // Construct using opamp.proto.Anyvalue.KeyValue.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getValueFieldBuilder();
        }
      }

      @Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        key_ = "";
        value_ = null;
        if (valueBuilder_ != null) {
          valueBuilder_.dispose();
          valueBuilder_ = null;
        }
        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return Anyvalue.internal_static_opamp_proto_KeyValue_descriptor;
      }

      @Override
      public KeyValue getDefaultInstanceForType() {
        return KeyValue.getDefaultInstance();
      }

      @Override
      public KeyValue build() {
        KeyValue result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public KeyValue buildPartial() {
        KeyValue result = new KeyValue(this);
        if (bitField0_ != 0) {
          buildPartial0(result);
        }
        onBuilt();
        return result;
      }

      private void buildPartial0(KeyValue result) {
        int from_bitField0_ = bitField0_;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          result.key_ = key_;
        }
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000002) != 0)) {
          result.value_ = valueBuilder_ == null ? value_ : valueBuilder_.build();
          to_bitField0_ |= 0x00000001;
        }
        result.bitField0_ |= to_bitField0_;
      }

      @Override
      public Builder clone() {
        return super.clone();
      }

      @Override
      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.setField(field, value);
      }

      @Override
      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @Override
      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof KeyValue) {
          return mergeFrom((KeyValue) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(KeyValue other) {
        if (other == KeyValue.getDefaultInstance()) return this;
        if (!other.getKey().isEmpty()) {
          key_ = other.key_;
          bitField0_ |= 0x00000001;
          onChanged();
        }
        if (other.hasValue()) {
          mergeValue(other.getValue());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10:
                {
                  key_ = input.readStringRequireUtf8();
                  bitField0_ |= 0x00000001;
                  break;
                } // case 10
              case 18:
                {
                  input.readMessage(getValueFieldBuilder().getBuilder(), extensionRegistry);
                  bitField0_ |= 0x00000002;
                  break;
                } // case 18
              default:
                {
                  if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                    done = true; // was an endgroup tag
                  }
                  break;
                } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }

      private int bitField0_;

      private Object key_ = "";

      /**
       * <code>string key = 1;</code>
       *
       * @return The key.
       */
      public String getKey() {
        Object ref = key_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          key_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>string key = 1;</code>
       *
       * @return The bytes for key.
       */
      public com.google.protobuf.ByteString getKeyBytes() {
        Object ref = key_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8((String) ref);
          key_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string key = 1;</code>
       *
       * @param value The key to set.
       * @return This builder for chaining.
       */
      public Builder setKey(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        key_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }

      /**
       * <code>string key = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearKey() {
        key_ = getDefaultInstance().getKey();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>string key = 1;</code>
       *
       * @param value The bytes for key to set.
       * @return This builder for chaining.
       */
      public Builder setKeyBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);
        key_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }

      private AnyValue value_;
      private com.google.protobuf.SingleFieldBuilderV3<
              AnyValue, AnyValue.Builder, AnyValueOrBuilder>
          valueBuilder_;

      /**
       * <code>.opamp.proto.AnyValue value = 2;</code>
       *
       * @return Whether the value field is set.
       */
      public boolean hasValue() {
        return ((bitField0_ & 0x00000002) != 0);
      }

      /**
       * <code>.opamp.proto.AnyValue value = 2;</code>
       *
       * @return The value.
       */
      public AnyValue getValue() {
        if (valueBuilder_ == null) {
          return value_ == null ? AnyValue.getDefaultInstance() : value_;
        } else {
          return valueBuilder_.getMessage();
        }
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public Builder setValue(AnyValue value) {
        if (valueBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          value_ = value;
        } else {
          valueBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public Builder setValue(AnyValue.Builder builderForValue) {
        if (valueBuilder_ == null) {
          value_ = builderForValue.build();
        } else {
          valueBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public Builder mergeValue(AnyValue value) {
        if (valueBuilder_ == null) {
          if (((bitField0_ & 0x00000002) != 0)
              && value_ != null
              && value_ != AnyValue.getDefaultInstance()) {
            getValueBuilder().mergeFrom(value);
          } else {
            value_ = value;
          }
        } else {
          valueBuilder_.mergeFrom(value);
        }
        if (value_ != null) {
          bitField0_ |= 0x00000002;
          onChanged();
        }
        return this;
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public Builder clearValue() {
        bitField0_ = (bitField0_ & ~0x00000002);
        value_ = null;
        if (valueBuilder_ != null) {
          valueBuilder_.dispose();
          valueBuilder_ = null;
        }
        onChanged();
        return this;
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public AnyValue.Builder getValueBuilder() {
        bitField0_ |= 0x00000002;
        onChanged();
        return getValueFieldBuilder().getBuilder();
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      public AnyValueOrBuilder getValueOrBuilder() {
        if (valueBuilder_ != null) {
          return valueBuilder_.getMessageOrBuilder();
        } else {
          return value_ == null ? AnyValue.getDefaultInstance() : value_;
        }
      }

      /** <code>.opamp.proto.AnyValue value = 2;</code> */
      private com.google.protobuf.SingleFieldBuilderV3<
              AnyValue, AnyValue.Builder, AnyValueOrBuilder>
          getValueFieldBuilder() {
        if (valueBuilder_ == null) {
          valueBuilder_ =
              new com.google.protobuf.SingleFieldBuilderV3<
                  AnyValue, AnyValue.Builder, AnyValueOrBuilder>(
                  getValue(), getParentForChildren(), isClean());
          value_ = null;
        }
        return valueBuilder_;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:opamp.proto.KeyValue)
    }

    // @@protoc_insertion_point(class_scope:opamp.proto.KeyValue)
    private static final KeyValue DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new KeyValue();
    }

    public static KeyValue getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KeyValue> PARSER =
        new com.google.protobuf.AbstractParser<KeyValue>() {
          @Override
          public KeyValue parsePartialFrom(
              com.google.protobuf.CodedInputStream input,
              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
              builder.mergeFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
              throw e.setUnfinishedMessage(builder.buildPartial());
            } catch (com.google.protobuf.UninitializedMessageException e) {
              throw e.asInvalidProtocolBufferException()
                  .setUnfinishedMessage(builder.buildPartial());
            } catch (java.io.IOException e) {
              throw new com.google.protobuf.InvalidProtocolBufferException(e)
                  .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
          }
        };

    public static com.google.protobuf.Parser<KeyValue> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<KeyValue> getParserForType() {
      return PARSER;
    }

    @Override
    public KeyValue getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }
  }

  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_opamp_proto_AnyValue_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_opamp_proto_AnyValue_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_opamp_proto_ArrayValue_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_opamp_proto_ArrayValue_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_opamp_proto_KeyValueList_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_opamp_proto_KeyValueList_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_opamp_proto_KeyValue_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_opamp_proto_KeyValue_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    String[] descriptorData = {
      "\n\016anyvalue.proto\022\013opamp.proto\"\350\001\n\010AnyVal"
          + "ue\022\026\n\014string_value\030\001 \001(\tH\000\022\024\n\nbool_value"
          + "\030\002 \001(\010H\000\022\023\n\tint_value\030\003 \001(\003H\000\022\026\n\014double_"
          + "value\030\004 \001(\001H\000\022.\n\013array_value\030\005 \001(\0132\027.opa"
          + "mp.proto.ArrayValueH\000\0221\n\014kvlist_value\030\006 "
          + "\001(\0132\031.opamp.proto.KeyValueListH\000\022\025\n\013byte"
          + "s_value\030\007 \001(\014H\000B\007\n\005value\"3\n\nArrayValue\022%"
          + "\n\006values\030\001 \003(\0132\025.opamp.proto.AnyValue\"5\n"
          + "\014KeyValueList\022%\n\006values\030\001 \003(\0132\025.opamp.pr"
          + "oto.KeyValue\"=\n\010KeyValue\022\013\n\003key\030\001 \001(\t\022$\n"
          + "\005value\030\002 \001(\0132\025.opamp.proto.AnyValueB.Z,g"
          + "ithub.com/open-telemetry/opamp-go/protob"
          + "ufsb\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
    internal_static_opamp_proto_AnyValue_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_opamp_proto_AnyValue_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_opamp_proto_AnyValue_descriptor,
            new String[] {
              "StringValue",
              "BoolValue",
              "IntValue",
              "DoubleValue",
              "ArrayValue",
              "KvlistValue",
              "BytesValue",
              "Value",
            });
    internal_static_opamp_proto_ArrayValue_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_opamp_proto_ArrayValue_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_opamp_proto_ArrayValue_descriptor,
            new String[] {
              "Values",
            });
    internal_static_opamp_proto_KeyValueList_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_opamp_proto_KeyValueList_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_opamp_proto_KeyValueList_descriptor,
            new String[] {
              "Values",
            });
    internal_static_opamp_proto_KeyValue_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_opamp_proto_KeyValue_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_opamp_proto_KeyValue_descriptor,
            new String[] {
              "Key", "Value",
            });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
