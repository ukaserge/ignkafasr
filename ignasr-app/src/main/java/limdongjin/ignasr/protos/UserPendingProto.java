// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: resources/protobuf/userpending.proto

package limdongjin.ignasr.protos;

public final class UserPendingProto {
  private UserPendingProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface UserPendingOrBuilder extends
      // @@protoc_insertion_point(interface_extends:limdongjin.ignasr.protos.UserPending)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string reqId = 1;</code>
     * @return The reqId.
     */
    java.lang.String getReqId();
    /**
     * <code>string reqId = 1;</code>
     * @return The bytes for reqId.
     */
    com.google.protobuf.ByteString
        getReqIdBytes();
  }
  /**
   * Protobuf type {@code limdongjin.ignasr.protos.UserPending}
   */
  public static final class UserPending extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:limdongjin.ignasr.protos.UserPending)
      UserPendingOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use UserPending.newBuilder() to construct.
    private UserPending(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private UserPending() {
      reqId_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new UserPending();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return limdongjin.ignasr.protos.UserPendingProto.internal_static_limdongjin_ignasr_protos_UserPending_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return limdongjin.ignasr.protos.UserPendingProto.internal_static_limdongjin_ignasr_protos_UserPending_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              limdongjin.ignasr.protos.UserPendingProto.UserPending.class, limdongjin.ignasr.protos.UserPendingProto.UserPending.Builder.class);
    }

    public static final int REQID_FIELD_NUMBER = 1;
    private volatile java.lang.Object reqId_;
    /**
     * <code>string reqId = 1;</code>
     * @return The reqId.
     */
    @java.lang.Override
    public java.lang.String getReqId() {
      java.lang.Object ref = reqId_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        reqId_ = s;
        return s;
      }
    }
    /**
     * <code>string reqId = 1;</code>
     * @return The bytes for reqId.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getReqIdBytes() {
      java.lang.Object ref = reqId_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        reqId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(reqId_)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, reqId_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(reqId_)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, reqId_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof limdongjin.ignasr.protos.UserPendingProto.UserPending)) {
        return super.equals(obj);
      }
      limdongjin.ignasr.protos.UserPendingProto.UserPending other = (limdongjin.ignasr.protos.UserPendingProto.UserPending) obj;

      if (!getReqId()
          .equals(other.getReqId())) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + REQID_FIELD_NUMBER;
      hash = (53 * hash) + getReqId().hashCode();
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static limdongjin.ignasr.protos.UserPendingProto.UserPending parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(limdongjin.ignasr.protos.UserPendingProto.UserPending prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code limdongjin.ignasr.protos.UserPending}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:limdongjin.ignasr.protos.UserPending)
        limdongjin.ignasr.protos.UserPendingProto.UserPendingOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return limdongjin.ignasr.protos.UserPendingProto.internal_static_limdongjin_ignasr_protos_UserPending_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return limdongjin.ignasr.protos.UserPendingProto.internal_static_limdongjin_ignasr_protos_UserPending_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                limdongjin.ignasr.protos.UserPendingProto.UserPending.class, limdongjin.ignasr.protos.UserPendingProto.UserPending.Builder.class);
      }

      // Construct using limdongjin.ignasr.protos.UserPendingProto.UserPending.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        reqId_ = "";

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return limdongjin.ignasr.protos.UserPendingProto.internal_static_limdongjin_ignasr_protos_UserPending_descriptor;
      }

      @java.lang.Override
      public limdongjin.ignasr.protos.UserPendingProto.UserPending getDefaultInstanceForType() {
        return limdongjin.ignasr.protos.UserPendingProto.UserPending.getDefaultInstance();
      }

      @java.lang.Override
      public limdongjin.ignasr.protos.UserPendingProto.UserPending build() {
        limdongjin.ignasr.protos.UserPendingProto.UserPending result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public limdongjin.ignasr.protos.UserPendingProto.UserPending buildPartial() {
        limdongjin.ignasr.protos.UserPendingProto.UserPending result = new limdongjin.ignasr.protos.UserPendingProto.UserPending(this);
        result.reqId_ = reqId_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof limdongjin.ignasr.protos.UserPendingProto.UserPending) {
          return mergeFrom((limdongjin.ignasr.protos.UserPendingProto.UserPending)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(limdongjin.ignasr.protos.UserPendingProto.UserPending other) {
        if (other == limdongjin.ignasr.protos.UserPendingProto.UserPending.getDefaultInstance()) return this;
        if (!other.getReqId().isEmpty()) {
          reqId_ = other.reqId_;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10: {
                reqId_ = input.readStringRequireUtf8();

                break;
              } // case 10
              default: {
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

      private java.lang.Object reqId_ = "";
      /**
       * <code>string reqId = 1;</code>
       * @return The reqId.
       */
      public java.lang.String getReqId() {
        java.lang.Object ref = reqId_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          reqId_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string reqId = 1;</code>
       * @return The bytes for reqId.
       */
      public com.google.protobuf.ByteString
          getReqIdBytes() {
        java.lang.Object ref = reqId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          reqId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string reqId = 1;</code>
       * @param value The reqId to set.
       * @return This builder for chaining.
       */
      public Builder setReqId(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        reqId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>string reqId = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearReqId() {
        
        reqId_ = getDefaultInstance().getReqId();
        onChanged();
        return this;
      }
      /**
       * <code>string reqId = 1;</code>
       * @param value The bytes for reqId to set.
       * @return This builder for chaining.
       */
      public Builder setReqIdBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        reqId_ = value;
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:limdongjin.ignasr.protos.UserPending)
    }

    // @@protoc_insertion_point(class_scope:limdongjin.ignasr.protos.UserPending)
    private static final limdongjin.ignasr.protos.UserPendingProto.UserPending DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new limdongjin.ignasr.protos.UserPendingProto.UserPending();
    }

    public static limdongjin.ignasr.protos.UserPendingProto.UserPending getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<UserPending>
        PARSER = new com.google.protobuf.AbstractParser<UserPending>() {
      @java.lang.Override
      public UserPending parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<UserPending> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<UserPending> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public limdongjin.ignasr.protos.UserPendingProto.UserPending getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_limdongjin_ignasr_protos_UserPending_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_limdongjin_ignasr_protos_UserPending_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n$resources/protobuf/userpending.proto\022\030" +
      "limdongjin.ignasr.protos\"\034\n\013UserPending\022" +
      "\r\n\005reqId\030\001 \001(\tB,\n\030limdongjin.ignasr.prot" +
      "osB\020UserPendingProtob\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_limdongjin_ignasr_protos_UserPending_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_limdongjin_ignasr_protos_UserPending_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_limdongjin_ignasr_protos_UserPending_descriptor,
        new java.lang.String[] { "ReqId", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
