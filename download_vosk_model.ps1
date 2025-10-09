$modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
$outputPath = "app/src/main/assets/model-en-us.zip"
$extractPath = "app/src/main/assets/model-en-us"

# Crear directorio si no existe
New-Item -ItemType Directory -Force -Path "app/src/main/assets"

# Descargar el modelo
Invoke-WebRequest -Uri $modelUrl -OutFile $outputPath

# Extraer el archivo ZIP
Expand-Archive -Path $outputPath -DestinationPath $extractPath -Force

# Eliminar el archivo ZIP después de extraer
Remove-Item $outputPath

Write-Host "Modelo Vosk descargado y extraído correctamente" 