// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: bo.proto

package run.mone.local.docean.protobuf;

public interface NacosReqProtoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:run.mone.local.docean.protobuf.NacosReqProto)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string cmd = 1;</code>
   * @return The cmd.
   */
  java.lang.String getCmd();
  /**
   * <code>string cmd = 1;</code>
   * @return The bytes for cmd.
   */
  com.google.protobuf.ByteString
      getCmdBytes();

  /**
   * <code>.run.mone.local.docean.protobuf.BeatInfoData beatinfo = 2;</code>
   * @return Whether the beatinfo field is set.
   */
  boolean hasBeatinfo();
  /**
   * <code>.run.mone.local.docean.protobuf.BeatInfoData beatinfo = 2;</code>
   * @return The beatinfo.
   */
  run.mone.local.docean.protobuf.BeatInfoData getBeatinfo();
  /**
   * <code>.run.mone.local.docean.protobuf.BeatInfoData beatinfo = 2;</code>
   */
  run.mone.local.docean.protobuf.BeatInfoDataOrBuilder getBeatinfoOrBuilder();

  /**
   * <code>string from = 3;</code>
   * @return The from.
   */
  java.lang.String getFrom();
  /**
   * <code>string from = 3;</code>
   * @return The bytes for from.
   */
  com.google.protobuf.ByteString
      getFromBytes();

  /**
   * <code>.run.mone.local.docean.protobuf.CheckSum checkSum = 4;</code>
   * @return Whether the checkSum field is set.
   */
  boolean hasCheckSum();
  /**
   * <code>.run.mone.local.docean.protobuf.CheckSum checkSum = 4;</code>
   * @return The checkSum.
   */
  run.mone.local.docean.protobuf.CheckSum getCheckSum();
  /**
   * <code>.run.mone.local.docean.protobuf.CheckSum checkSum = 4;</code>
   */
  run.mone.local.docean.protobuf.CheckSumOrBuilder getCheckSumOrBuilder();

  /**
   * <code>bool showErrorMessage = 5;</code>
   * @return The showErrorMessage.
   */
  boolean getShowErrorMessage();

  /**
   * <code>string source = 6;</code>
   * @return The source.
   */
  java.lang.String getSource();
  /**
   * <code>string source = 6;</code>
   * @return The bytes for source.
   */
  com.google.protobuf.ByteString
      getSourceBytes();

  /**
   * <code>repeated string datumKeys = 7;</code>
   * @return A list containing the datumKeys.
   */
  java.util.List<java.lang.String>
      getDatumKeysList();
  /**
   * <code>repeated string datumKeys = 7;</code>
   * @return The count of datumKeys.
   */
  int getDatumKeysCount();
  /**
   * <code>repeated string datumKeys = 7;</code>
   * @param index The index of the element to return.
   * @return The datumKeys at the given index.
   */
  java.lang.String getDatumKeys(int index);
  /**
   * <code>repeated string datumKeys = 7;</code>
   * @param index The index of the value to return.
   * @return The bytes of the datumKeys at the given index.
   */
  com.google.protobuf.ByteString
      getDatumKeysBytes(int index);

  /**
   * <code>map&lt;string, .run.mone.local.docean.protobuf.InstancesProto&gt; instances = 8;</code>
   */
  int getInstancesCount();
  /**
   * <code>map&lt;string, .run.mone.local.docean.protobuf.InstancesProto&gt; instances = 8;</code>
   */
  boolean containsInstances(
      java.lang.String key);
  /**
   * Use {@link #getInstancesMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, run.mone.local.docean.protobuf.InstancesProto>
  getInstances();
  /**
   * <code>map&lt;string, .run.mone.local.docean.protobuf.InstancesProto&gt; instances = 8;</code>
   */
  java.util.Map<java.lang.String, run.mone.local.docean.protobuf.InstancesProto>
  getInstancesMap();
  /**
   * <code>map&lt;string, .run.mone.local.docean.protobuf.InstancesProto&gt; instances = 8;</code>
   */

  run.mone.local.docean.protobuf.InstancesProto getInstancesOrDefault(
      java.lang.String key,
      run.mone.local.docean.protobuf.InstancesProto defaultValue);
  /**
   * <code>map&lt;string, .run.mone.local.docean.protobuf.InstancesProto&gt; instances = 8;</code>
   */

  run.mone.local.docean.protobuf.InstancesProto getInstancesOrThrow(
      java.lang.String key);
}