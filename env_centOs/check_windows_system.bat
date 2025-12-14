@echo off
chcp 65001 >nul
REM 如果需要，取消注释下一行以尝试使用 GBK (936) 编码
REM chcp 936 >nul

echo ===========================================
echo Windows System Hardware / Software Configuration Report
echo Time: %date% %time%
echo ===========================================
echo.

echo --- OS / System Info ---
systeminfo | findstr /B /C:"OS Name" /C:"OS Version" /C:"System Type" /C:"Host Name" /C:"System Boot Time"
echo.

echo --- CPU Info ---
wmic cpu get Name,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed /format:list
echo.

echo --- Memory Info (Physical + Available) ---
wmic OS get TotalVisibleMemorySize,FreePhysicalMemory,FreeVirtualMemory /format:list
echo.

echo --- Physical Disk Info ---
wmic diskdrive get DeviceID,Model,InterfaceType,Size,MediaType,Status /format:list
echo.

echo --- Logical Drives (Volumes / Partitions) Info ---
wmic logicaldisk get DeviceID,VolumeName,FileSystem,Size,FreeSpace,DriveType /format:list
echo.

echo --- Disk & Partition Layout (via diskpart) ---
(
  echo list disk
  echo list volume
  echo exit
) > "%temp%\dp.txt"
diskpart /s "%temp%\dp.txt" | findstr /C:"Disk" /C:"Volume" /C:"Partition" /C:"Status" /C:"Size" /C:"Type"
del "%temp%\dp.txt"
echo.

echo --- Network Adapters (IP / MAC) Info ---
ipconfig /all
echo.

echo ===========================================
echo Check completed. Please review above output.
pause
