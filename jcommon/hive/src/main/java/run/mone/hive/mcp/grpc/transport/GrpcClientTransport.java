package run.mone.hive.mcp.grpc.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import run.mone.hive.mcp.grpc.*;
import run.mone.hive.mcp.spec.ClientMcpTransport;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.hive.mcp.spec.McpSchema.JSONRPCMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static run.mone.hive.mcp.spec.McpSchema.METHOD_TOOLS_CALL;
import static run.mone.hive.mcp.spec.McpSchema.METHOD_TOOLS_STREAM;

/**
 * goodjava@qq.com
 * gRPC 客户端传输层实现
 */
@Slf4j
public class GrpcClientTransport implements ClientMcpTransport {

    private final String host;
    private final int port;
    private final ObjectMapper objectMapper;
    private ManagedChannel channel;
    private McpServiceGrpc.McpServiceBlockingStub blockingStub;
    private McpServiceGrpc.McpServiceStub asyncStub;

    private Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> messageHandler;

    /**
     * 创建 gRPC 客户端传输层
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     */
    public GrpcClientTransport(String host, int port) {
        this.host = host;
        this.port = port;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        return Mono.fromRunnable(() -> {
            this.messageHandler = handler;
            this.channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    // 启用自动重连
                    .enableRetry()
                    // 设置重连参数
//                    .defaultServiceConfig(Map.of(
//                            "methodConfig", List.of(Map.of(
//                                    "name", List.of(Map.of(
//                                            "service", "yourservice.YourService"  // 替换为您的服务名
//                                    )),
//                                    "retryPolicy", Map.of(
//                                            "maxAttempts", 5.0,
//                                            "initialBackoff", "1s",
//                                            "maxBackoff", "30s",
//                                            "backoffMultiplier", 2.0,
//                                            "retryableStatusCodes", List.of(
//                                                    "UNAVAILABLE",
//                                                    "UNKNOWN"
//                                            )
//                                    )
//                            ))
//                    ))

                    .build();
            this.blockingStub = McpServiceGrpc.newBlockingStub(channel);
            this.asyncStub = McpServiceGrpc.newStub(channel);
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            try {
                if (channel != null) {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public Mono<Object> sendMessage(JSONRPCMessage message) {
        return Mono.create((sink) -> {
            try {
                if (message instanceof run.mone.hive.mcp.spec.McpSchema.JSONRPCRequest request) {
                    handleToolCall(request, sink);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Flux<Object> sendStreamMessage(JSONRPCMessage message) {
        return Flux.create(sink -> {
            if (message instanceof run.mone.hive.mcp.spec.McpSchema.JSONRPCRequest request) {
                handleToolStreamCall(request, sink);
            }
        });
    }

    //发送ping消息到服务端
    public PingResponse ping(PingRequest request) {
        return blockingStub.ping(request);
    }

    public StreamObserver<StreamRequest> observer(StreamObserver<StreamResponse> observer, String clientId) {
        // 创建带重连功能的包装观察者
        StreamObserver<StreamResponse> reconnectingObserver = new StreamObserver<StreamResponse>() {
            @Override
            public void onNext(StreamResponse response) {
                // 直接转发响应
                observer.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("连接错误: " + t.getMessage() + "，5秒后重连...");

                // 5秒后重连
                try {
                    Thread.sleep(5000);
                    log.info("正在重新连接...");
                    observer(observer, clientId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    observer.onError(e);
                }
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        };


        StreamObserver<StreamRequest> req = asyncStub.bidirectionalToolStream(reconnectingObserver);
        req.onNext(StreamRequest.newBuilder().setName("observer").setClientId(clientId).build());
        return req;
    }

    @SuppressWarnings("unchecked")
    private void handleToolCall(run.mone.hive.mcp.spec.McpSchema.JSONRPCRequest request, MonoSink sink) {
        McpSchema.CallToolRequest re = (McpSchema.CallToolRequest) request.params();
        Map<String, Object> objectMap = re.arguments();

        Map<String, String> stringMap = objectMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Objects.toString(e.getValue(), null)
                ));

        String methodName = getMethodName(request);

        CallToolRequest grpcRequest = CallToolRequest.newBuilder()
                .setName(METHOD_TOOLS_CALL)
                .setMethod(methodName)
                .setClientId(request.clientId())
                .putAllArguments(stringMap)
                .build();

        // 发送请求并处理响应
        CallToolResponse response = blockingStub.callTool(grpcRequest);
        sink.success(response);
    }

    private static String getMethodName(McpSchema.JSONRPCRequest request) {
        String methodName = "";
        if (request.params() instanceof McpSchema.CallToolRequest ctr) {
            methodName = ctr.name();
        }
        return methodName;
    }

    private void handleToolStreamCall(run.mone.hive.mcp.spec.McpSchema.JSONRPCRequest request, FluxSink sink) {
        McpSchema.CallToolRequest re = (McpSchema.CallToolRequest) request.params();
        Map<String, Object> objectMap = re.arguments();

        Map<String, String> stringMap = objectMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Objects.toString(e.getValue(), null)
                ));

        String methodName = getMethodName(request);

        //protobuf map 只能是 <string,string>
        CallToolRequest req = CallToolRequest.newBuilder()
                .setName(METHOD_TOOLS_STREAM)
                .putAllArguments(stringMap)
                .setMethod(methodName)
                .setClientId(request.clientId()).build();
        this.asyncStub.callToolStream(req, new StreamObserver<>() {
            @Override
            public void onNext(CallToolResponse callToolResponse) {
                sink.next(callToolResponse);
            }

            @Override
            public void onError(Throwable throwable) {
                sink.error(throwable);
            }

            @Override
            public void onCompleted() {
                sink.complete();
            }
        });

    }

    //grpc 的返回结果,需要手动转换下
    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        try {
            if (data instanceof CallToolResponse ctr) {
                return (T) new McpSchema.CallToolResult(ctr.getContentList().stream().map(it -> new McpSchema.TextContent(it.getText().getText(), it.getText().getData())).collect(Collectors.toUnmodifiableList()), false);
            }

            if (data instanceof PingResponse pr) {
                return (T) pr;
            }

            if (data instanceof String) {
                return objectMapper.readValue((String) data, typeRef);
            } else {
                return objectMapper.convertValue(data, typeRef);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error unmarshalling data", e);
        }
    }
} 