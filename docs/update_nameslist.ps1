# Picks up updates to NamesList.txt from a neighbouring clone of the charts repository.
# This should be run from the root of the unicodetools repository, as
# .\docs\update_nameslist.ps1.

$ErrorActionPreference = "Stop"

pushd ..\charts
git fetch
$versions = git log --pretty=format:"%H" main -- .\nomenclator\output\NamesList.txt
for ($i = 0; $i -lt $versions.Length; ++$i) {
    git checkout $versions[$i] .\nomenclator\output\NamesList.txt
    if (-not (Compare-Object                                     `
                (Get-Content .\nomenclator\output\NamesList.txt) `
                (Get-Content ..\unicodetools\unicodetools\data\ucd\dev\NamesList.txt))) {
        Write-Host "unicodetools has $($versions[$i]); $i commits to import…"
        break
    }
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
    git commit -m $message
    git commit --amend --author=$author --no-edit
    git commit --amend --date=$date --no-edit
    pushd ..\charts
}
popd
