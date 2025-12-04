package com.logix.common.util;

import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.*;

/**
 * 网络工具类
 *
 * @author Kanade
 * @since 2025/09/24
 */
@Slf4j
public final class NetworkUtils {

    private NetworkUtils() {
    }

    /**
     * 缓存的本地IP地址
     */
    private static final String CACHED_LOCAL_IP = findLocalIP();

    /**
     * 默认忽略的网络接口 (参考Spring配置)
     */
    private static final List<String> DEFAULT_IGNORED_INTERFACES = Arrays.asList(
            "docker.*", "veth.*", "br-.*", "virbr.*", "vmnet.*", "vboxnet.*"
    );

    /**
     * 获取本地服务器IP地址
     *
     * @return 本地IP地址
     */
    public static String getLocalIP() {
        return CACHED_LOCAL_IP;
    }

    /**
     * 查找本地IP地
     */
    private static String findLocalIP() {
        InetAddress result = findFirstNonLoopbackAddress();
        if (result != null) {
            return result.getHostAddress();
        }

        // Spring式兜底策略
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("无法获取localhost地址，使用127.0.0.1");
            return "127.0.0.1";
        }
    }

    /**
     * 查找第一个非回环地址
     */
    private static InetAddress findFirstNonLoopbackAddress() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(ifc -> {
                        try {
                            return ifc.isUp() && !ignoreInterface(ifc.getDisplayName());
                        } catch (Exception e) {
                            log.trace("检查接口状态失败: {}", ifc.getDisplayName());
                            return false;
                        }
                    })
                    .sorted((ifc1, ifc2) -> Integer.compare(ifc1.getIndex(), ifc2.getIndex()))
                    .flatMap(ifc -> {
                        log.trace("测试网络接口: {} (index: {})", ifc.getDisplayName(), ifc.getIndex());
                        return Collections.list(ifc.getInetAddresses()).stream();
                    })
                    .filter(addr -> addr instanceof Inet4Address
                            && !addr.isLoopbackAddress()
                            && !ignoreAddress(addr))
                    .peek(addr -> log.trace("找到有效IP地址: {}", addr.getHostAddress()))
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("无法获取第一个非回环地址", e);
            return null;
        }
    }

    /**
     * 判断是否忽略指定网络接口
     */
    private static boolean ignoreInterface(String interfaceName) {
        return DEFAULT_IGNORED_INTERFACES.stream()
                .anyMatch(interfaceName::matches);
    }

    /**
     * 判断是否忽略指定IP地址
     */
    private static boolean ignoreAddress(InetAddress address) {
        // 过滤链路本地地址 (169.254.x.x)
        if (address.isLinkLocalAddress()) {
            log.trace("忽略链路本地地址: {}", address.getHostAddress());
            return true;
        }

        return false;
    }

}