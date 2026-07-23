# Picks up updates to UCD files maintained by the charts WG from a neighbouring
# clone of the charts repository.
# By default, this picks up NamesList.txt, but it can be used for files
# maintained in the chart editor’s UCD overrides directory.
# This Powershell should be run from the root of the unicodetools repository, as
# .\docs\update_charts_files.ps1 [filename.txt].
param (
    [string]$file = "NamesList.txt"
)

if ($file -eq "NamesList.txt") {
    $chartsPath = ".\nomenclator\output\NamesList.txt"
} else {
    $chartsPath = ".\ucd-editor\$file"
}

$ErrorActionPreference = "Stop"

pushd ..\charts
git fetch
git checkout origin/main
$versions = git log --pretty=format:"%H" origin/main -- $chartsPath
# Go back in time in the charts repository until we find a version of
# the file that matches the one from unicodetools.
for ($i = 0; $i -lt $versions.Length; ++$i) {
    git checkout $versions[$i] $chartsPath
    if (-not (Compare-Object                                     `
                (Get-Content $chartsPath) `
                (Get-Content ..\unicodetools\unicodetools\data\ucd\dev\$file))) {
        Write-Host "unicodetools has $($versions[$i]); $i commits to import…"
        break
    }
}
if ($i -eq $versions.Length) {
    popd
    Write-Error ("Could not find charts revision matching unicodetools "     `
    + "$file.  If $file has been modified in the tools, "    `
    + "check out the tools before that revision and run this script again, " `
    + "then merge.")
}
while (--$i -ge 0) {
    $version = $versions[$i]
    $author = git show -s --format='%an <%ae>' $version
    $date = git show -s --format='%aD' $version
    $message = git show -s --format='%B' $version
    git checkout $versions[$i] $chartsPath
    cp $chartsPath ..\unicodetools\unicodetools\data\ucd\dev\$file
    popd
    git add .\unicodetools\data\ucd\dev\$file
    git commit -m "$message"    `
               --author=$author `
               --date=$date
    pushd ..\charts
}
popd
