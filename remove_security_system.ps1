# Script para remover el sistema de seguridad temporal
# Ejecutar ANTES de publicar en Google Play Store

Write-Host "=== REMOVIENDO SISTEMA DE SEGURIDAD TEMPORAL ===" -ForegroundColor Red
Write-Host ""

# Verificar que estamos en el directorio correcto
if (-not (Test-Path "app/src/main/java/com/example/speak/SecurityActivity.java")) {
    Write-Host "ERROR: No se encontró SecurityActivity.java" -ForegroundColor Red
    Write-Host "Asegúrate de ejecutar este script desde el directorio raíz del proyecto" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Proyecto encontrado correctamente" -ForegroundColor Green
Write-Host ""

# Confirmar acción
Write-Host "ADVERTENCIA: Este script eliminará completamente el sistema de seguridad temporal." -ForegroundColor Yellow
Write-Host "Esto es necesario para publicar en Google Play Store." -ForegroundColor Yellow
Write-Host ""
$confirmation = Read-Host "¿Estás seguro de que quieres continuar? (s/N)"

if ($confirmation -ne "s" -and $confirmation -ne "S") {
    Write-Host "Operación cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Iniciando proceso de eliminación..." -ForegroundColor Cyan

# 1. Eliminar archivos Java
Write-Host "1. Eliminando archivos Java..." -ForegroundColor Cyan
$javaFiles = @(
    "app/src/main/java/com/example/speak/SecurityActivity.java",
    "app/src/main/java/com/example/speak/SecurityConfigActivity.java",
    "app/src/main/java/com/example/speak/helpers/SecurityManager.java"
)

foreach ($file in $javaFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "   ✓ Eliminado: $file" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ No encontrado: $file" -ForegroundColor Yellow
    }
}

# 2. Eliminar archivos de layout
Write-Host "2. Eliminando archivos de layout..." -ForegroundColor Cyan
$layoutFiles = @(
    "app/src/main/res/layout/activity_security.xml",
    "app/src/main/res/layout/activity_security_config.xml"
)

foreach ($file in $layoutFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "   ✓ Eliminado: $file" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ No encontrado: $file" -ForegroundColor Yellow
    }
}

# 3. Verificar si existe el directorio helpers y eliminarlo si está vacío
Write-Host "3. Verificando directorio helpers..." -ForegroundColor Cyan
$helpersDir = "app/src/main/java/com/example/speak/helpers"
if (Test-Path $helpersDir) {
    $files = Get-ChildItem $helpersDir -File
    if ($files.Count -eq 0) {
        Remove-Item $helpersDir -Force
        Write-Host "   ✓ Directorio helpers eliminado (estaba vacío)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Directorio helpers no está vacío, verificar manualmente" -ForegroundColor Yellow
    }
}

# 4. Actualizar AndroidManifest.xml
Write-Host "4. Actualizando AndroidManifest.xml..." -ForegroundColor Cyan
$manifestPath = "app/src/main/AndroidManifest.xml"

if (Test-Path $manifestPath) {
    $manifestContent = Get-Content $manifestPath -Raw
    
    # Remover SecurityActivity y SecurityConfigActivity
    $manifestContent = $manifestContent -replace '<activity\s+android:name="\.SecurityActivity"[^>]*>.*?</activity>', ''
    $manifestContent = $manifestContent -replace '<activity\s+android:name="\.SecurityConfigActivity"[^>]*>.*?</activity>', ''
    
    # Restaurar MajorActivity como actividad principal
    $manifestContent = $manifestContent -replace 'android:name="\.MajorActivity"\s+android:exported="false"', 'android:name=".MajorActivity" android:exported="true"'
    
    # Agregar intent-filter a MajorActivity si no existe
    if ($manifestContent -notmatch 'android:name="\.MajorActivity"[^>]*>[\s\S]*?<intent-filter>') {
        $manifestContent = $manifestContent -replace '(<activity\s+android:name="\.MajorActivity"[^>]*>)', '$1
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>'
    }
    
    # Guardar cambios
    Set-Content $manifestPath $manifestContent -Encoding UTF8
    Write-Host "   ✓ AndroidManifest.xml actualizado" -ForegroundColor Green
} else {
    Write-Host "   ❌ ERROR: No se encontró AndroidManifest.xml" -ForegroundColor Red
}

# 5. Eliminar archivos de documentación
Write-Host "5. Eliminando archivos de documentación..." -ForegroundColor Cyan
$docFiles = @(
    "SISTEMA_SEGURIDAD_TEMPORAL.md",
    "remove_security_system.ps1"
)

foreach ($file in $docFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "   ✓ Eliminado: $file" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "=== PROCESO COMPLETADO ===" -ForegroundColor Green
Write-Host ""
Write-Host "✓ Sistema de seguridad temporal eliminado completamente" -ForegroundColor Green
Write-Host "✓ MajorActivity restaurada como actividad principal" -ForegroundColor Green
Write-Host "✓ AndroidManifest.xml actualizado" -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANTE:" -ForegroundColor Yellow
Write-Host "- Verifica que la aplicación compile correctamente" -ForegroundColor Yellow
Write-Host "- Prueba que el flujo de la aplicación funcione sin problemas" -ForegroundColor Yellow
Write-Host "- Ahora puedes publicar en Google Play Store" -ForegroundColor Green
Write-Host ""
Write-Host "Presiona cualquier tecla para continuar..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
