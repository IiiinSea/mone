package run.mone.hive.llm;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QwenProxyTest {

    @BeforeEach
    void setUp() {
        // 设置环境变量
        System.setProperty("DASHSCOPE_API_KEY", "sk-297ec3d7309748b1b3c05b80d2207416");
    }

    @Test
    void testCallWithMessage_Success() throws ApiException, NoApiKeyException, InputRequiredException {
        // 执行方法
        GenerationResult result = QwenProxy.callWithMessage();
        System.out.println(result);
        // 验证结果不为空
        assertNotNull(result);
        // 验证返回的消息不为空
        assertNotNull(result.getOutput().getChoices());
        assertFalse(result.getOutput().getChoices().isEmpty());
    }

    @Test
    void testCallWithMessage_NoApiKey() {
        // 清除API Key
        System.clearProperty("DASHSCOPE_API_KEY");
        
        // 验证抛出NoApiKeyException异常
        assertThrows(NoApiKeyException.class, () -> {
            QwenProxy.callWithMessage();
        });
    }

    @Test
    void testCallWithMessage_ApiException() throws ApiException, NoApiKeyException, InputRequiredException {
        try (MockedStatic<Generation> generationMock = Mockito.mockStatic(Generation.class)) {
            // 模拟Generation类抛出异常
            Generation mockGen = mock(Generation.class);
            when(mockGen.call(any())).thenThrow(new RuntimeException("API调用失败"));
            generationMock.when(Generation::new).thenReturn(mockGen);

            // 验证抛出异常
            assertThrows(RuntimeException.class, () -> {
                QwenProxy.callWithMessage();
            });
        }
    }

    @Test
    void testCallWithMessage_InputRequiredException() throws ApiException, NoApiKeyException, InputRequiredException {
        try (MockedStatic<Generation> generationMock = Mockito.mockStatic(Generation.class)) {
            // 模拟Generation类抛出InputRequiredException
            Generation mockGen = mock(Generation.class);
            when(mockGen.call(any())).thenThrow(new InputRequiredException("输入参数缺失"));
            generationMock.when(Generation::new).thenReturn(mockGen);

            // 验证抛出InputRequiredException异常
            assertThrows(InputRequiredException.class, () -> {
                QwenProxy.callWithMessage();
            });
        }
    }
} 