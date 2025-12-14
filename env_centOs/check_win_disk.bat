# ============================================
# 脚本功能：检查系统所有物理磁盘及挂载情况
# 仅做检测，不修改磁盘
# ============================================

$disks = Get-Disk

foreach ($disk in $disks) {
    Write-Host "-----------------------------------"
    Write-Host "磁盘编号: $($disk.Number) 名称: $($disk.FriendlyName)"
    Write-Host "状态: $($disk.OperationalStatus) 分区类型: $($disk.PartitionStyle)"
    Write-Host "是否脱机: $($disk.IsOffline) 是否只读: $($disk.IsReadOnly)"

    # 获取磁盘分区信息
    $partitions = Get-Partition -DiskNumber $disk.Number -ErrorAction SilentlyContinue

    if (!$partitions -or $partitions.Count -eq 0) {
        Write-Host "⚠️ 该磁盘无分区或未挂载盘符"
    }
    else {
        Write-Host "分区信息："
        foreach ($p in $partitions) {
            $driveLetter = if ($p.DriveLetter) { $p.DriveLetter } else { "无盘符" }
            Write-Host "  分区编号: $($p.PartitionNumber) 盘符: $driveLetter 大小: $([math]::Round($p.Size/1GB,2)) GB"
        }
    }
}

Write-Host "==================================="
Write-Host "磁盘检查完毕！"
