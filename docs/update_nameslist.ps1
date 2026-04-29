# Picks up updates to NamesList.txt from a neighbouring clone of the charts repository.
# This Powershell should be run from the root of the unicodetools repository, as
# .\docs\update_nameslist.ps1.

$ErrorActionPreference = "Stop"

pushd ..\charts
git fetch
git checkout origin/main
$versions = git log --pretty=format:"%H" origin/main -- .\nomenclator\output\NamesList.txt
# Go back in time in the charts repository until we find a version of
# NamesList.txt that matches the one from unicodetools.
for ($i = 0; $i -lt $versions.Length; ++$i) {
    git checkout $versions[$i] .\nomenclator\output\NamesList.txt
    if (-not (Compare-Object                                     `
                (Get-Content .\nomenclator\output\NamesList.txt) `
                (Get-Content ..\unicodetools\unicodetools\data\ucd\dev\NamesList.txt))) {
        Write-Host "unicodetools has $($versions[$i]); $i commits to import…"
        break
    }
}
if ($i -eq $versions.Length) {
    popd
    Write-Error ("Could not find charts revision matching unicodetools "     `
    + "NamesList.txt.  If NamesList.txt has been modified in the tools, "    `
    + "check out the tools before that revision and run this script again, " `
    + "then merge.")
}
while (--$i -ge 0) {
    $version = $versions[$i]
    $author = git show -s --format='%an <%ae>' $version
    $date = git show -s --format='%aD' $version
    $message = git show -s --format='%B' $version
    git checkout $versions[$i] .\nomenclator\output\NamesList.txt
    cp .\nomenclator\output\NamesList.txt ..\unicodetools\unicodetools\data\ucd\dev\NamesList.txt
    popd
    git add .\unicodetools\data\ucd\dev\NamesList.txt
    git commit -m $message      `
               --author=$author `
               --date=$date
    pushd ..\charts
}
popd
