Write-Host "Organizing Kotlin files based on their package..."

Get-ChildItem -Recurse -Filter *.kt | ForEach-Object {
    $filePath = $_.FullName
    $content = Get-Content $filePath | Select-String '^package '
    
    if ($content) {
        $packageLine = $content.Line.Trim()
        $package = $packageLine -replace '^package ', ''
        $packagePath = $package -replace '\.', '\'
        $projectRoot = "app\src\main\java"
        $targetDir = Join-Path $projectRoot $packagePath
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir | Out-Null
        }

        $fileName = $_.Name
        $destination = Join-Path $targetDir $fileName

        if ($_.FullName -ne $destination) {
            Move-Item -Path $_.FullName -Destination $destination
            Write-Host "Moved $fileName to $targetDir"
        }
    }
}

Write-Host "All Kotlin files reorganized."
