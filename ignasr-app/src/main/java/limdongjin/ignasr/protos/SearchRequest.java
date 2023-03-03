// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: resources/protobuf/search_request.proto

package limdongjin.ignasr.protos;

/**
 * Protobuf type {@code limdongjin.ignasr.protos.SearchRequest}
 */
public final class SearchRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:limdongjin.ignasr.protos.SearchRequest)
    SearchRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use SearchRequest.newBuilder() to construct.
  private SearchRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private SearchRequest() {
    reqId_ = "";
    userId_ = "";
    keyword_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new SearchRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return limdongjin.ignasr.protos.SearchRequestProto.internal_static_limdongjin_ignasr_protos_SearchRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return limdongjin.ignasr.protos.SearchRequestProto.internal_static_limdongjin_ignasr_protos_SearchRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            limdongjin.ignasr.protos.SearchRequest.class, limdongjin.ignasr.protos.SearchRequest.Builder.class);
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

  public static final int USERID_FIELD_NUMBER = 2;
  private volatile java.lang.Object userId_;
  /**
   * <code>string userId = 2;</code>
   * @return The userId.
   */
  @java.lang.Override
  public java.lang.String getUserId() {
    java.lang.Object ref = userId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      userId_ = s;
      return s;
    }
  }
  /**
   * <code>string userId = 2;</code>
   * @return The bytes for userId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getUserIdBytes() {
    java.lang.Object ref = userId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      userId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int KEYWORD_FIELD_NUMBER = 3;
  private volatile java.lang.Object keyword_;
  /**
   * <code>string keyword = 3;</code>
   * @return The keyword.
   */
  @java.lang.Override
  public java.lang.String getKeyword() {
    java.lang.Object ref = keyword_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      keyword_ = s;
      return s;
    }
  }
  /**
   * <code>string keyword = 3;</code>
   * @return The bytes for keyword.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getKeywordBytes() {
    java.lang.Object ref = keyword_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      keyword_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int LIMITNUM_FIELD_NUMBER = 4;
  private int limitNum_;
  /**
   * <code>int32 limitNum = 4;</code>
   * @return The limitNum.
   */
  @java.lang.Override
  public int getLimitNum() {
    return limitNum_;
  }

  public static final int LIMITDURATIONSECONDS_FIELD_NUMBER = 5;
  private int limitDurationSeconds_;
  /**
   * <code>int32 limitDurationSeconds = 5;</code>
   * @return The limitDurationSeconds.
   */
  @java.lang.Override
  public int getLimitDurationSeconds() {
    return limitDurationSeconds_;
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
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(userId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, userId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(keyword_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, keyword_);
    }
    if (limitNum_ != 0) {
      output.writeInt32(4, limitNum_);
    }
    if (limitDurationSeconds_ != 0) {
      output.writeInt32(5, limitDurationSeconds_);
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
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(userId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, userId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(keyword_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, keyword_);
    }
    if (limitNum_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(4, limitNum_);
    }
    if (limitDurationSeconds_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(5, limitDurationSeconds_);
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
    if (!(obj instanceof limdongjin.ignasr.protos.SearchRequest)) {
      return super.equals(obj);
    }
    limdongjin.ignasr.protos.SearchRequest other = (limdongjin.ignasr.protos.SearchRequest) obj;

    if (!getReqId()
        .equals(other.getReqId())) return false;
    if (!getUserId()
        .equals(other.getUserId())) return false;
    if (!getKeyword()
        .equals(other.getKeyword())) return false;
    if (getLimitNum()
        != other.getLimitNum()) return false;
    if (getLimitDurationSeconds()
        != other.getLimitDurationSeconds()) return false;
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
    hash = (37 * hash) + USERID_FIELD_NUMBER;
    hash = (53 * hash) + getUserId().hashCode();
    hash = (37 * hash) + KEYWORD_FIELD_NUMBER;
    hash = (53 * hash) + getKeyword().hashCode();
    hash = (37 * hash) + LIMITNUM_FIELD_NUMBER;
    hash = (53 * hash) + getLimitNum();
    hash = (37 * hash) + LIMITDURATIONSECONDS_FIELD_NUMBER;
    hash = (53 * hash) + getLimitDurationSeconds();
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static limdongjin.ignasr.protos.SearchRequest parseFrom(
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
  public static Builder newBuilder(limdongjin.ignasr.protos.SearchRequest prototype) {
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
   * Protobuf type {@code limdongjin.ignasr.protos.SearchRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:limdongjin.ignasr.protos.SearchRequest)
      limdongjin.ignasr.protos.SearchRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return limdongjin.ignasr.protos.SearchRequestProto.internal_static_limdongjin_ignasr_protos_SearchRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return limdongjin.ignasr.protos.SearchRequestProto.internal_static_limdongjin_ignasr_protos_SearchRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              limdongjin.ignasr.protos.SearchRequest.class, limdongjin.ignasr.protos.SearchRequest.Builder.class);
    }

    // Construct using limdongjin.ignasr.protos.SearchRequest.newBuilder()
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

      userId_ = "";

      keyword_ = "";

      limitNum_ = 0;

      limitDurationSeconds_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return limdongjin.ignasr.protos.SearchRequestProto.internal_static_limdongjin_ignasr_protos_SearchRequest_descriptor;
    }

    @java.lang.Override
    public limdongjin.ignasr.protos.SearchRequest getDefaultInstanceForType() {
      return limdongjin.ignasr.protos.SearchRequest.getDefaultInstance();
    }

    @java.lang.Override
    public limdongjin.ignasr.protos.SearchRequest build() {
      limdongjin.ignasr.protos.SearchRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public limdongjin.ignasr.protos.SearchRequest buildPartial() {
      limdongjin.ignasr.protos.SearchRequest result = new limdongjin.ignasr.protos.SearchRequest(this);
      result.reqId_ = reqId_;
      result.userId_ = userId_;
      result.keyword_ = keyword_;
      result.limitNum_ = limitNum_;
      result.limitDurationSeconds_ = limitDurationSeconds_;
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
      if (other instanceof limdongjin.ignasr.protos.SearchRequest) {
        return mergeFrom((limdongjin.ignasr.protos.SearchRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(limdongjin.ignasr.protos.SearchRequest other) {
      if (other == limdongjin.ignasr.protos.SearchRequest.getDefaultInstance()) return this;
      if (!other.getReqId().isEmpty()) {
        reqId_ = other.reqId_;
        onChanged();
      }
      if (!other.getUserId().isEmpty()) {
        userId_ = other.userId_;
        onChanged();
      }
      if (!other.getKeyword().isEmpty()) {
        keyword_ = other.keyword_;
        onChanged();
      }
      if (other.getLimitNum() != 0) {
        setLimitNum(other.getLimitNum());
      }
      if (other.getLimitDurationSeconds() != 0) {
        setLimitDurationSeconds(other.getLimitDurationSeconds());
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
            case 18: {
              userId_ = input.readStringRequireUtf8();

              break;
            } // case 18
            case 26: {
              keyword_ = input.readStringRequireUtf8();

              break;
            } // case 26
            case 32: {
              limitNum_ = input.readInt32();

              break;
            } // case 32
            case 40: {
              limitDurationSeconds_ = input.readInt32();

              break;
            } // case 40
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

    private java.lang.Object userId_ = "";
    /**
     * <code>string userId = 2;</code>
     * @return The userId.
     */
    public java.lang.String getUserId() {
      java.lang.Object ref = userId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        userId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string userId = 2;</code>
     * @return The bytes for userId.
     */
    public com.google.protobuf.ByteString
        getUserIdBytes() {
      java.lang.Object ref = userId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        userId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string userId = 2;</code>
     * @param value The userId to set.
     * @return This builder for chaining.
     */
    public Builder setUserId(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      userId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string userId = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearUserId() {
      
      userId_ = getDefaultInstance().getUserId();
      onChanged();
      return this;
    }
    /**
     * <code>string userId = 2;</code>
     * @param value The bytes for userId to set.
     * @return This builder for chaining.
     */
    public Builder setUserIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      userId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object keyword_ = "";
    /**
     * <code>string keyword = 3;</code>
     * @return The keyword.
     */
    public java.lang.String getKeyword() {
      java.lang.Object ref = keyword_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        keyword_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string keyword = 3;</code>
     * @return The bytes for keyword.
     */
    public com.google.protobuf.ByteString
        getKeywordBytes() {
      java.lang.Object ref = keyword_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        keyword_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string keyword = 3;</code>
     * @param value The keyword to set.
     * @return This builder for chaining.
     */
    public Builder setKeyword(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      keyword_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string keyword = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearKeyword() {
      
      keyword_ = getDefaultInstance().getKeyword();
      onChanged();
      return this;
    }
    /**
     * <code>string keyword = 3;</code>
     * @param value The bytes for keyword to set.
     * @return This builder for chaining.
     */
    public Builder setKeywordBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      keyword_ = value;
      onChanged();
      return this;
    }

    private int limitNum_ ;
    /**
     * <code>int32 limitNum = 4;</code>
     * @return The limitNum.
     */
    @java.lang.Override
    public int getLimitNum() {
      return limitNum_;
    }
    /**
     * <code>int32 limitNum = 4;</code>
     * @param value The limitNum to set.
     * @return This builder for chaining.
     */
    public Builder setLimitNum(int value) {
      
      limitNum_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 limitNum = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearLimitNum() {
      
      limitNum_ = 0;
      onChanged();
      return this;
    }

    private int limitDurationSeconds_ ;
    /**
     * <code>int32 limitDurationSeconds = 5;</code>
     * @return The limitDurationSeconds.
     */
    @java.lang.Override
    public int getLimitDurationSeconds() {
      return limitDurationSeconds_;
    }
    /**
     * <code>int32 limitDurationSeconds = 5;</code>
     * @param value The limitDurationSeconds to set.
     * @return This builder for chaining.
     */
    public Builder setLimitDurationSeconds(int value) {
      
      limitDurationSeconds_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 limitDurationSeconds = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearLimitDurationSeconds() {
      
      limitDurationSeconds_ = 0;
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


    // @@protoc_insertion_point(builder_scope:limdongjin.ignasr.protos.SearchRequest)
  }

  // @@protoc_insertion_point(class_scope:limdongjin.ignasr.protos.SearchRequest)
  private static final limdongjin.ignasr.protos.SearchRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new limdongjin.ignasr.protos.SearchRequest();
  }

  public static limdongjin.ignasr.protos.SearchRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SearchRequest>
      PARSER = new com.google.protobuf.AbstractParser<SearchRequest>() {
    @java.lang.Override
    public SearchRequest parsePartialFrom(
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

  public static com.google.protobuf.Parser<SearchRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SearchRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public limdongjin.ignasr.protos.SearchRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
