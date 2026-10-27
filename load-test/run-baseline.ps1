param(
    [string]$K6 = 'k6',
    [ValidateSet('movie-read','booking','contention')][string]$Scenario = 'movie-read',
    [int[]]$Levels = @(10,25,50),
    [int]$Repeats = 3,
    [int]$ApiProcessId = 0,
    [int[]]$DatabaseProcessIds = @()
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
if ($Repeats -lt 3) { throw 'Use at least three repeats.' }
if ($Scenario -eq 'contention' -and $Levels.Count * $Repeats -gt 9) { throw 'Contention needs a distinct fixture seat for each run; at most nine runs are available.' }
$dirtySource = git status --porcelain --untracked-files=all -- src/main pom.xml load-test
if ($dirtySource) { throw 'Commit the source and load scripts before recording a baseline, so the commit identifies what was measured.' }
if ($Levels | Where-Object { $_ -lt 1 }) { throw 'VU levels must be positive.' }
$env:WARMUP_SECONDS = '30'
$env:DURATION_SECONDS = '120'
$env:GIT_COMMIT = git rev-parse HEAD
$runRoot = Join-Path (Get-Location).Path ('load-test/results/' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $runRoot | Out-Null
if (-not ('LoadTestMemory.MemoryStatusEx' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace LoadTestMemory {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
    public class MemoryStatusEx {
        public uint dwLength;
        public uint dwMemoryLoad;
        public ulong ullTotalPhys;
        public ulong ullAvailPhys;
        public ulong ullTotalPageFile;
        public ulong ullAvailPageFile;
        public ulong ullTotalVirtual;
        public ulong ullAvailVirtual;
        public ulong ullAvailExtendedVirtual;
        public MemoryStatusEx() { dwLength = (uint)Marshal.SizeOf(typeof(MemoryStatusEx)); }
    }
    public static class NativeMethods {
        [DllImport("kernel32.dll", SetLastError = true)]
        public static extern bool GlobalMemoryStatusEx([In, Out] MemoryStatusEx status);
    }
}
'@
}
$memory = [LoadTestMemory.MemoryStatusEx]::new()
if (-not [LoadTestMemory.NativeMethods]::GlobalMemoryStatusEx($memory)) { throw 'Cannot read system memory size.' }
$cpuName = (Get-ItemPropertyValue -Path 'HKLM:\HARDWARE\DESCRIPTION\System\CentralProcessor\0' -Name 'ProcessorNameString' -ErrorAction SilentlyContinue)
if (-not $cpuName) { $cpuName = $env:PROCESSOR_IDENTIFIER }
if (-not $cpuName) { $cpuName = 'unknown' }
$environment = [ordered]@{collectedAt=[DateTime]::UtcNow.ToString('o');gitCommit=$env:GIT_COMMIT;
    cpu=$cpuName;logicalProcessors=[Environment]::ProcessorCount;ramGiB=[Math]::Round($memory.ullTotalPhys/1GB,2);
    os=[Environment]::OSVersion.VersionString;scenario=$Scenario;warmupSeconds=30;measureSeconds=120;
    apiProcessId=$ApiProcessId;databaseProcessIds=$DatabaseProcessIds;gitDirty=[bool](git status --porcelain --untracked-files=no)}
$environment | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runRoot 'environment.json') -Encoding UTF8
foreach ($level in $Levels) {
    foreach ($repeat in 1..$Repeats) {
        $name = "$Scenario-$($level)vu-run$repeat"
        $env:VUS = "$level"
        $env:SUMMARY_PATH = Join-Path $runRoot "$name.json"
        if ($Scenario -eq 'contention') {
            $env:CONTENTION_RUN_INDEX = "$(([Array]::IndexOf($Levels, $level) * $Repeats) + $repeat)"
        }
        $client = Start-Process -FilePath $K6 -ArgumentList @('run','--quiet',('-'+'-out'),('"json=' + (Join-Path $runRoot "$name-raw.json") + '"'),"load-test/$Scenario.js") -WindowStyle Hidden -PassThru -WorkingDirectory (Get-Location).Path -RedirectStandardOutput (Join-Path $runRoot "$name.log") -RedirectStandardError (Join-Path $runRoot "$name-error.log")
        $samples = [System.Collections.Generic.List[object]]::new()
        $previousCpu = @{}
        $previousTime = [DateTime]::UtcNow
        while (!$client.HasExited) {
            $now = [DateTime]::UtcNow
            $elapsed = [Math]::Max(($now-$previousTime).TotalSeconds,0.001)
            $databaseIds = [System.Collections.Generic.HashSet[int]]::new()
            foreach ($rootId in $DatabaseProcessIds) { [void]$databaseIds.Add($rootId) }
            if ($DatabaseProcessIds.Count) {
                foreach ($candidate in @(Get-Process -Name 'postgres' -ErrorAction SilentlyContinue)) {
                    [void]$databaseIds.Add([int]$candidate.Id)
                }
            }
            $groups = @{api=@($ApiProcessId);database=@($databaseIds);k6=@($client.Id)}
            foreach ($group in @('api','database','k6')) {
                $cpuDelta = 0.0; $privateBytes = 0L; $workingBytes = 0L; $available = $false
                foreach ($processIdToSample in $groups[$group]) {
                    if (!$processIdToSample) { continue }
                    $sample = Get-Process -Id $processIdToSample -ErrorAction SilentlyContinue
                    if (!$sample) { continue }
                    try {
                        $sample.Refresh()
                        $cpu = $sample.TotalProcessorTime.TotalSeconds
                        $samplePrivate = $sample.PrivateMemorySize64
                        $sampleWorking = $sample.WorkingSet64
                    } catch { continue }
                    $available = $true
                    if ($previousCpu.ContainsKey($sample.Id)) { $cpuDelta += [Math]::Max(0.0,$cpu-$previousCpu[$sample.Id]) }
                    $previousCpu[$sample.Id] = $cpu
                    $privateBytes += $samplePrivate; $workingBytes += $sampleWorking
                }
                $samples.Add([pscustomobject]@{timestampUtc=$now.ToString('o');group=$group;available=$available;
                    cpuPercentMachine=[Math]::Round(100*$cpuDelta/$elapsed/[Environment]::ProcessorCount,3);
                    privateMiB=[Math]::Round($privateBytes/1MB,3);workingSetMiB=[Math]::Round($workingBytes/1MB,3)})
            }
            $previousTime = $now; Start-Sleep -Seconds 1; $client.Refresh()
        }
        $client.WaitForExit()
        $samples | Export-Csv -LiteralPath (Join-Path $runRoot "$name-resources.csv") -NoTypeInformation -Encoding UTF8
        if ($client.ExitCode -ne 0) { throw "k6 failed: $name. Results are retained in $runRoot" }
        Write-Output "Completed $name"
    }
}
Write-Output "Results: $runRoot"
