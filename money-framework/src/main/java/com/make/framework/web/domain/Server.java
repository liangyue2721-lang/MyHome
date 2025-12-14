package com.make.framework.web.domain;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import com.make.common.utils.Arith;
import com.make.common.utils.ip.IpUtils;
import com.make.framework.web.domain.server.Cpu;
import com.make.framework.web.domain.server.Jvm;
import com.make.framework.web.domain.server.Mem;
import com.make.framework.web.domain.server.Sys;
import com.make.framework.web.domain.server.SysFile;
import com.make.framework.web.domain.server.ThreadPoolInfo;
import com.make.framework.web.domain.server.ClusterThreadPoolInfo;
import com.make.framework.web.domain.server.NetworkTraffic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

/**
 * 服务器相关信息
 *
 * @author ruoyi
 */
@Component
public class Server
{
    private static final int OSHI_WAIT_SECOND = 1000;

    /**
     * CPU相关信息
     */
    private Cpu cpu = new Cpu();

    /**
     * 內存相关信息
     */
    private Mem mem = new Mem();

    /**
     * JVM相关信息
     */
    private Jvm jvm = new Jvm();

    /**
     * 服务器相关信息
     */
    private Sys sys = new Sys();

    /**
     * 磁盘相关信息
     */
    private List<SysFile> sysFiles = new LinkedList<SysFile>();

    /**
     * 线程池相关信息
     */
    @Autowired
    private ThreadPoolInfo threadPoolInfo;

    /**
     * 集群线程池信息
     */
    @Autowired
    private ClusterThreadPoolInfo clusterThreadPoolInfo;

    /**
     * 网络流量信息
     */
    private NetworkTraffic networkTraffic = new NetworkTraffic();

    public Cpu getCpu()
    {
        return cpu;
    }

    public void setCpu(Cpu cpu)
    {
        this.cpu = cpu;
    }

    public Mem getMem()
    {
        return mem;
    }

    public void setMem(Mem mem)
    {
        this.mem = mem;
    }

    public Jvm getJvm()
    {
        return jvm;
    }

    public void setJvm(Jvm jvm)
    {
        this.jvm = jvm;
    }

    public Sys getSys()
    {
        return sys;
    }

    public void setSys(Sys sys)
    {
        this.sys = sys;
    }

    public List<SysFile> getSysFiles()
    {
        return sysFiles;
    }

    public void setSysFiles(List<SysFile> sysFiles)
    {
        this.sysFiles = sysFiles;
    }

    public void copyTo() throws Exception
    {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        setCpuInfo(hal.getProcessor());

        setMemInfo(hal.getMemory());

        setSysInfo();

        setJvmInfo();

        setSysFiles(si.getOperatingSystem());
        
        setNetworkTrafficInfo(hal);

        // 初始化线程池信息
        threadPoolInfo.init();
    }

    /**
     * 设置CPU信息
     */
    private void setCpuInfo(CentralProcessor processor)
    {
        // CPU信息
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(OSHI_WAIT_SECOND);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long cSys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        cpu.setCpuNum(processor.getLogicalProcessorCount());
        cpu.setTotal(totalCpu);
        cpu.setSys(cSys);
        cpu.setUsed(user);
        cpu.setWait(iowait);
        cpu.setFree(idle);
    }

    /**
     * 设置内存信息
     */
    private void setMemInfo(GlobalMemory memory)
    {
        mem.setTotal(memory.getTotal());
        mem.setUsed(memory.getTotal() - memory.getAvailable());
        mem.setFree(memory.getAvailable());
    }

    /**
     * 设置服务器信息
     */
    private void setSysInfo()
    {
        Properties props = System.getProperties();
        sys.setComputerName(IpUtils.getHostName());
        sys.setComputerIp(IpUtils.getHostIp());
        sys.setOsName(props.getProperty("os.name"));
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));
    }

    /**
     * 设置Java虚拟机
     */
    private void setJvmInfo() throws UnknownHostException
    {
        Properties props = System.getProperties();
        jvm.setTotal(Runtime.getRuntime().totalMemory());
        jvm.setMax(Runtime.getRuntime().maxMemory());
        jvm.setFree(Runtime.getRuntime().freeMemory());
        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
    }

    /**
     * 设置磁盘信息
     */
    private void setSysFiles(OperatingSystem os)
    {
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        sysFiles.clear(); // 清空旧数据，防止重复叠加
        for (OSFileStore fs : fsArray)
        {
            long free = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            long used = total - free;
            SysFile sysFile = new SysFile();
            sysFile.setDirName(fs.getMount());
            sysFile.setSysTypeName(fs.getType());
            sysFile.setTypeName(fs.getName());
            sysFile.setTotal(convertFileSize(total));
            sysFile.setFree(convertFileSize(free));
            sysFile.setUsed(convertFileSize(used));
            sysFile.setUsage(Arith.mul(Arith.div(used, total, 4), 100));
            sysFiles.add(sysFile);
        }
    }

    /**
     * 设置网络流量信息
     */
    private void setNetworkTrafficInfo(HardwareAbstractionLayer hal) {
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        List<NetworkTraffic.NetworkInterfaceInfo> interfaceInfos = networkIFs.stream().map(net -> {
            NetworkTraffic.NetworkInterfaceInfo info = new NetworkTraffic.NetworkInterfaceInfo();
            // 添加null检查以防止NullPointerException
            info.setName(net.getName() != null ? net.getName() : "unknown");
            info.setDisplayName(net.getDisplayName() != null ? net.getDisplayName() : "unknown");
            info.setIpv4Addr(net.getIPv4addr() != null ? net.getIPv4addr() : new String[0]);
            info.setBytesRecv(net.getBytesRecv());
            info.setBytesSent(net.getBytesSent());
            info.setPacketsRecv(net.getPacketsRecv());
            info.setPacketsSent(net.getPacketsSent());
            // 计算流量速率（这里简单设置为1秒间隔）
            info.calculateRate(1);
            return info;
        }).filter(info -> info.getName() != null && !info.getName().startsWith("lo")) // 过滤掉null和本地回环接口
          .collect(Collectors.toList());

        networkTraffic.setInterfaces(interfaceInfos);
    }
    
    public NetworkTraffic getNetworkTraffic() {
        return networkTraffic;
    }

    public void setNetworkTraffic(NetworkTraffic networkTraffic) {
        this.networkTraffic = networkTraffic;
    }

    public ThreadPoolInfo getThreadPoolInfo() {
        return threadPoolInfo;
    }

    public void setThreadPoolInfo(ThreadPoolInfo threadPoolInfo) {
        this.threadPoolInfo = threadPoolInfo;
    }

    /**
     * 字节转换
     *
     * @param size 字节大小
     * @return 转换后值
     */
    public String convertFileSize(long size)
    {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb)
        {
            return String.format("%.1f GB", (float) size / gb);
        }
        else if (size >= mb)
        {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        }
        else if (size >= kb)
        {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        }
        else
        {
            return String.format("%d B", size);
        }
    }

    /**
     * 获取集群环境下所有节点的线程池信息
     *
     * @return 所有节点的线程池信息
     */
    public Map<String, Map<String, Object>> getClusterThreadPoolInfo() {
        return clusterThreadPoolInfo.getAllNodeThreadPoolInfo();
    }
}