package com.make.framework.web.domain.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络流量信息实体类
 */
public class NetworkTraffic {
    /**
     * 网络接口信息列表
     */
    private List<NetworkInterfaceInfo> interfaces;
    
    /**
     * 存储上一次网络流量数据，用于计算流量变化率
     */
    private static final Map<String, NetworkInterfaceInfo> previousTrafficData = new ConcurrentHashMap<>();

    public List<NetworkInterfaceInfo> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<NetworkInterfaceInfo> interfaces) {
        this.interfaces = interfaces;
    }

    /**
     * 网络接口信息
     */
    public static class NetworkInterfaceInfo {
        /**
         * 接口名称
         */
        private String name;

        /**
         * 接口描述
         */
        private String displayName;

        /**
         * IPv4地址
         */
        private String[] ipv4Addr;

        /**
         * 接收字节数
         */
        private long bytesRecv;

        /**
         * 发送字节数
         */
        private long bytesSent;

        /**
         * 接收数据包数
         */
        private long packetsRecv;

        /**
         * 发送数据包数
         */
        private long packetsSent;
        
        /**
         * 接收速率（字节/秒）
         */
        private double receiveRate = 0.0;
        
        /**
         * 发送速率（字节/秒）
         */
        private double sendRate = 0.0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String[] getIpv4Addr() {
            return ipv4Addr;
        }

        public void setIpv4Addr(String[] ipv4Addr) {
            this.ipv4Addr = ipv4Addr;
        }

        public long getBytesRecv() {
            return bytesRecv;
        }

        public void setBytesRecv(long bytesRecv) {
            this.bytesRecv = bytesRecv;
        }

        public long getBytesSent() {
            return bytesSent;
        }

        public void setBytesSent(long bytesSent) {
            this.bytesSent = bytesSent;
        }

        public long getPacketsRecv() {
            return packetsRecv;
        }

        public void setPacketsRecv(long packetsRecv) {
            this.packetsRecv = packetsRecv;
        }

        public long getPacketsSent() {
            return packetsSent;
        }

        public void setPacketsSent(long packetsSent) {
            this.packetsSent = packetsSent;
        }
        
        public double getReceiveRate() {
            return receiveRate;
        }
        
        public void setReceiveRate(double receiveRate) {
            this.receiveRate = receiveRate;
        }
        
        public double getSendRate() {
            return sendRate;
        }
        
        public void setSendRate(double sendRate) {
            this.sendRate = sendRate;
        }
        
        /**
         * 计算流量变化率
         * @param timeInterval 时间间隔（秒）
         */
        public void calculateRate(long timeInterval) {
            NetworkInterfaceInfo previous = previousTrafficData.get(this.name);
            if (previous != null) {
                // 计算接收速率（字节/秒）
                long recvDiff = this.bytesRecv - previous.bytesRecv;
                this.receiveRate = timeInterval > 0 ? (double) recvDiff / timeInterval : 0;
                
                // 计算发送速率（字节/秒）
                long sentDiff = this.bytesSent - previous.bytesSent;
                this.sendRate = timeInterval > 0 ? (double) sentDiff / timeInterval : 0;
            }
            
            // 更新历史数据
            previousTrafficData.put(this.name, clone());
        }
        
        /**
         * 克隆当前对象
         * @return NetworkInterfaceInfo的克隆对象
         */
        public NetworkInterfaceInfo clone() {
            NetworkInterfaceInfo cloned = new NetworkInterfaceInfo();
            cloned.name = this.name;
            cloned.displayName = this.displayName;
            // 修复可能的null指针异常
            cloned.ipv4Addr = this.ipv4Addr != null ? this.ipv4Addr.clone() : new String[0];
            cloned.bytesRecv = this.bytesRecv;
            cloned.bytesSent = this.bytesSent;
            cloned.packetsRecv = this.packetsRecv;
            cloned.packetsSent = this.packetsSent;
            return cloned;
        }
        
        @Override
        public String toString() {
            return "NetworkInterfaceInfo{" +
                    "name='" + name + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", ipv4Addr=" + java.util.Arrays.toString(ipv4Addr) +
                    ", bytesRecv=" + bytesRecv +
                    ", bytesSent=" + bytesSent +
                    ", packetsRecv=" + packetsRecv +
                    ", packetsSent=" + packetsSent +
                    ", receiveRate=" + receiveRate +
                    ", sendRate=" + sendRate +
                    '}';
        }
    }
}