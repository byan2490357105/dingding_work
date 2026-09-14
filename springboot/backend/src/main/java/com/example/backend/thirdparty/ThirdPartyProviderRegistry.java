package com.example.backend.thirdparty;

import com.example.backend.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方平台注册中心：按 provider 分发到具体实现。
 */
@Component
public class ThirdPartyProviderRegistry {

    private final Map<String, ThirdPartyProvider> providerMap = new LinkedHashMap<>();

    public ThirdPartyProviderRegistry(List<ThirdPartyProvider> providers) {
        for (ThirdPartyProvider provider : providers) {
            providerMap.put(provider.getProvider(), provider);
        }
    }

    /** 获取可用平台，未启用（未配置密钥）的平台不会返回给前端 */
    public List<ThirdPartyProvider> enabledProviders() {
        List<ThirdPartyProvider> result = new ArrayList<>();
        for (ThirdPartyProvider provider : providerMap.values()) {
            if (provider.isEnabled()) {
                result.add(provider);
            }
        }
        return result;
    }

    /** 获取指定平台，不存在或未启用时抛出业务异常 */
    public ThirdPartyProvider require(String provider) {
        ThirdPartyProvider target = providerMap.get(provider);
        if (target == null) {
            throw new BusinessException(400, "不支持的第三方平台：" + provider);
        }
        if (!target.isEnabled()) {
            throw new BusinessException(400, target.getDisplayName() + " 登录未启用，请先配置应用密钥");
        }
        return target;
    }
}
