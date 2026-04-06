package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.ServerMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerMetricService {

    private final MonitoringDataHolder dataHolder;
    private final SystemInfo systemInfo = new SystemInfo();

    public ServerMetric collect() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        // CPU usage (blocks for ~1 second to measure)
        double cpuLoad = processor.getSystemCpuLoad(1000L) * 100.0;
        int cpuCores = processor.getLogicalProcessorCount();

        // Memory
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long memoryTotal = memory.getTotal();
        long memoryAvailable = memory.getAvailable();
        long memoryUsed = memoryTotal - memoryAvailable;

        // Disk - use root file store
        long diskTotal = 0;
        long diskUsed = 0;
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        for (OSFileStore store : fileStores) {
            // Use the first meaningful file store (root on Linux, C: on Windows)
            if (store.getMount().equals("/") || store.getMount().startsWith("C:")) {
                diskTotal = store.getTotalSpace();
                diskUsed = diskTotal - store.getUsableSpace();
                break;
            }
        }
        // Fallback: if no root found, use the first file store
        if (diskTotal == 0 && !fileStores.isEmpty()) {
            OSFileStore first = fileStores.get(0);
            diskTotal = first.getTotalSpace();
            diskUsed = diskTotal - first.getUsableSpace();
        }

        ServerMetric metric = ServerMetric.builder()
                .cpuUsage(Math.round(cpuLoad * 100.0) / 100.0)
                .cpuCores(cpuCores)
                .memoryUsedBytes(memoryUsed)
                .memoryTotalBytes(memoryTotal)
                .diskUsedBytes(diskUsed)
                .diskTotalBytes(diskTotal)
                .collectedAt(LocalDateTime.now())
                .build();

        dataHolder.updateServerMetric(metric);
        return metric;
    }
}
