#######################################
### User config
#######################################
$csiroInstallExe = "$PSScriptRoot\workspace-drop\csiro.au-workspace-vs2017-5.0.4-12241.AMD64.exe"


#######################################
### Util functions
#######################################

### function to get the list of already installed apps
function Get-InstalledApps
{
    if ([IntPtr]::Size -eq 4) {
        $regpath = 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
    }
    else {
        $regpath = @(
            'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
            'HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*'
        )
    }
    Get-ItemProperty $regpath | .{process{if($_.DisplayName -and $_.UninstallString) { $_ } }} | Select DisplayName, Publisher, InstallDate, DisplayVersion, UninstallString |Sort DisplayName
}


#######################################
### Main script starts here
#######################################

# 1. Assumes a powershell running as admin
# 2. Assumes that E: drive is already mapped to \\VBOXSVR\au-flood-evacuation.
#    If not, try 'net use E: \\VBOXSVR\au-flood-evacuation'

### change to the integration directory
cd e:\integration

### if CSIRO workspace is installed, then first uninstall it
$appToMatch = "CSIRO Workspace"
$result = Get-InstalledApps | where {$_.DisplayName -like "*$appToMatch*"}
If ($result -ne $null) {
  Write-Host "$appToMatch is already installed; found $result"
  Write-Host "uninstalling $appToMatch now ..."
  $uninst = $result.UninstallString
  Start-Process cmd -ArgumentList "/c `"$uninst`" /S /quiet /norestart" -NoNewWindow -Wait
  Write-Host "finished uninstalling $appToMatch"
}

### Install CSIRO workspace
$result = Get-InstalledApps | where {$_.DisplayName -like "*$appToMatch*"}
If ($result -eq $null) {
    Write-Host  "installing CSIRO Workspace from $csiroInstallExe ..."
    Invoke-Expression "$csiroInstallExe /S | Out-Null"
    Write-Output "finished installing CSIRO Workspace"
}

### Run the workflow
Start-Process cmd -ArgumentList "/c `"C:\Program Files\csiro.au\workspace\bin\workspace-batch.exe`" --globalNameFile .\RunMatsim4FloodEvacuationTestGlobalNames.xml .\RunMatsim4FloodEvacuationTest.wsx" -NoNewWindow -Wait
