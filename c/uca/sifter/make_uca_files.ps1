$ErrorActionPreference = "Stop"

$vswhere = "${Env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
$msbuild =  &$vswhere                      `
    -requires Microsoft.Component.MSBuild  `
    -find MSBuild\**\Bin\amd64\MSBuild.exe `
    -latest

&$msbuild /t:"Clean;Build" /m /property:Configuration=Release /property:Platform=x64 sifter.sln
.\x64\Release\tests.exe
if (-not $?) {
    Write-Error "unisifeggs unit tests failed."
}
.\x64\Release\sifter.exe -t
Copy-Item .\decomps.txt ..\..\..\unicodetools\data\uca\dev\
.\x64\Release\sifter.exe -s
Copy-Item .\allkeys.txt ..\..\..\unicodetools\data\uca\dev\
Copy-Item .\ctt.txt ..\..\..\unicodetools\data\uca\dev\
$non_date_differences = git diff --unified=0             `
    | Select-String -NotMatch "^(\+\+\+|---) [ab]/"      `
    | Select-String "^[+-]"                              `
    | Select-String                                      `
        -NotMatch "^[+-][#%] Date: \d\d\d\d-\d\d-\d\d, \d\d:\d\d:\d\d GMT"
if ($non_date_differences.length -ne 0) {
    git diff
    Write-Error "Running the sifter resulted in non-date changes:`n$(
        [String]::Join("`n", $non_date_differences))"
}