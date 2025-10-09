# DOCUMENTO DE REQUERIMIENTOS DE SOFTWARE
## ColSpeak Armada - Aplicación Educativa de Inglés

---

**Información del Documento**
- **Proyecto:** ColSpeak Armada
- **Versión del Documento:** 1.0
- **Fecha:** Diciembre 2024
- **Cliente:** Armada Nacional de Colombia
- **Desarrollador:** Equipo de Desarrollo ColSpeak
- **Estado:** Aprobado

---

## 1. INTRODUCCIÓN

### 1.1 Propósito
Este documento especifica los requerimientos funcionales y no funcionales para el desarrollo de la aplicación móvil ColSpeak Armada, una plataforma educativa diseñada para el aprendizaje del idioma inglés siguiendo los estándares del Marco Común Europeo de Referencia (MCER) nivel A1.

### 1.2 Alcance
La aplicación está dirigida a miembros de la Armada Nacional de Colombia para mejorar sus competencias en inglés como segunda lengua, proporcionando un entorno de aprendizaje interactivo, progresivo y adaptado a las necesidades militares.

### 1.3 Definiciones y Acrónimos
- **MCER:** Marco Común Europeo de Referencia para las lenguas
- **A1:** Nivel básico de competencia lingüística
- **TTS:** Text-to-Speech (Síntesis de voz)
- **ASR:** Automatic Speech Recognition (Reconocimiento automático de voz)
- **Vosk:** Motor de reconocimiento de voz offline
- **Firebase:** Plataforma de desarrollo de aplicaciones de Google

---

## 2. DESCRIPCIÓN GENERAL

### 2.1 Perspectiva del Producto
ColSpeak Armada es una aplicación móvil independiente que opera en dispositivos Android, integrando tecnologías de reconocimiento de voz, síntesis de voz, y sistemas de evaluación automática para crear una experiencia de aprendizaje completa.

### 2.2 Funcionalidades del Producto
- **Aprendizaje Progresivo:** Sistema de niveles con desbloqueo secuencial
- **Cuatro Habilidades Lingüísticas:** Listening, Speaking, Reading, Writing
- **Evaluación Automática:** Sistema de quizzes y evaluación de pronunciación
- **Modo Libre:** Acceso sin restricciones para instructores
- **Seguimiento de Progreso:** Historial detallado de actividades
- **Autenticación Segura:** Integración con Firebase Auth

### 2.3 Clases de Usuario
- **Estudiantes:** Personal militar que aprende inglés
- **Instructores:** Personal docente que supervisa el progreso
- **Administradores:** Personal técnico que gestiona la aplicación

---

## 3. REQUERIMIENTOS FUNCIONALES

### 3.1 Gestión de Usuarios

#### RF-001: Registro de Usuario
**Prioridad:** Crítica
**Descripción:** El sistema debe permitir el registro de nuevos usuarios con información básica.
**Criterios de Aceptación:**
- El usuario puede ingresar nombre completo, email y contraseña
- El sistema valida formato de email y fortaleza de contraseña
- Se almacena información de fecha de registro
- Se genera ID único para cada usuario
- Se sincroniza con Firebase Authentication

#### RF-002: Autenticación de Usuario
**Prioridad:** Crítica
**Descripción:** El sistema debe autenticar usuarios existentes de forma segura.
**Criterios de Aceptación:**
- Login con email y contraseña
- Validación de credenciales con Firebase Auth
- Manejo de sesiones persistentes
- Logout seguro con limpieza de datos sensibles
- Recuperación de contraseña por email

#### RF-003: Gestión de Perfil
**Prioridad:** Alta
**Descripción:** Los usuarios pueden gestionar su información personal y preferencias.
**Criterios de Aceptación:**
- Visualización de información personal
- Actualización de datos de perfil
- Configuración de modo libre para instructores
- Visualización de estadísticas de progreso
- Gestión de preferencias de aplicación

### 3.2 Sistema de Navegación

#### RF-004: Pantalla de Inicio
**Prioridad:** Alta
**Descripción:** La aplicación debe mostrar una pantalla de bienvenida con video institucional.
**Criterios de Aceptación:**
- Reproducción automática de video institucional
- Duración basada en duración real del video
- Pantalla completa inmersiva
- Manejo de errores con fallback
- Transición suave a menú principal

#### RF-005: Carousel Principal
**Prioridad:** Alta
**Descripción:** Navegación principal mediante carousel horizontal de módulos.
**Criterios de Aceptación:**
- Carousel con ViewPager2
- 6 módulos principales: Quiz, Listening, Speaking, Reading, Writing, Word Order
- Indicadores visuales de progreso
- Navegación fluida entre módulos
- Integración con modo libre

### 3.3 Sistema de Progresión

#### RF-006: Control de Acceso Progresivo
**Prioridad:** Crítica
**Descripción:** El sistema debe controlar el acceso a temas según el progreso del usuario.
**Criterios de Aceptación:**
- Desbloqueo secuencial de temas
- Dependencias entre módulos (Writing → Reading → Speaking)
- Almacenamiento de progreso en SharedPreferences
- Desbloqueo automático al aprobar quizzes (≥70%)
- Efectos visuales para elementos bloqueados

#### RF-007: Modo Libre
**Prioridad:** Media
**Descripción:** Los instructores pueden activar modo libre para acceso sin restricciones.
**Criterios de Aceptación:**
- Toggle en perfil de instructor
- Acceso completo a todos los módulos
- Badges visuales indicativos
- Desactivación desde cualquier módulo
- Refresco automático de efectos visuales

### 3.4 Módulo de Listening

#### RF-008: Comprensión Auditiva
**Prioridad:** Alta
**Descripción:** Desarrollo de habilidades de comprensión auditiva.
**Criterios de Aceptación:**
- 7 temas con 3 niveles de dificultad
- Reproducción de audio nativo
- Ejercicios de identificación de imágenes
- Evaluación automática de respuestas
- Historial de resultados

#### RF-009: Mapas Interactivos de Listening
**Prioridad:** Alta
**Descripción:** Navegación por temas mediante mapas expandibles.
**Criterios de Aceptación:**
- Expansión/colapso de secciones
- Animaciones suaves (fade in/out)
- Validación de progreso antes de acceso
- Mensajes informativos sobre requisitos
- Iconografía temática naval

### 3.5 Módulo de Speaking

#### RF-010: Práctica de Pronunciación
**Prioridad:** Alta
**Descripción:** Desarrollo de habilidades de expresión oral.
**Criterios de Aceptación:**
- 7 temas de pronunciación
- Reconocimiento de voz con Vosk
- Evaluación automática de pronunciación
- Modelos de audio nativos
- Progresión secuencial entre temas

#### RF-011: Sistema de Evaluación de Pronunciación
**Prioridad:** Alta
**Descripción:** Evaluación automática de la pronunciación del usuario.
**Criterios de Aceptación:**
- Comparación con modelos de referencia
- Puntuación numérica (0-100)
- Feedback inmediato al usuario
- Almacenamiento de resultados
- Historial de prácticas

### 3.6 Módulo de Reading

#### RF-012: Comprensión Lectora
**Prioridad:** Alta
**Descripción:** Desarrollo de habilidades de comprensión lectora.
**Criterios de Aceptación:**
- Textos adaptados al nivel A1
- Sistema de traducción palabra por palabra
- Vocabulario contextual con definiciones
- Progresión gradual de dificultad
- Ejercicios de comprensión

#### RF-013: Sistema de Traducción
**Prioridad:** Media
**Descripción:** Ayuda contextual con traducciones y definiciones.
**Criterios de Aceptación:**
- Traducción al hacer tap en palabras
- Definiciones contextuales
- Pronunciación de palabras
- Vocabulario destacado
- Integración con ejercicios

### 3.7 Módulo de Writing

#### RF-014: Expresión Escrita
**Prioridad:** Alta
**Descripción:** Desarrollo de habilidades de expresión escrita.
**Criterios de Aceptación:**
- Ejercicios de escritura guiada
- Validación automática de respuestas
- Progresión secuencial de temas
- Dependencias con módulo Reading
- Feedback inmediato

### 3.8 Módulo de Word Order

#### RF-015: Orden de Palabras
**Prioridad:** Media
**Descripción:** Práctica de construcción de oraciones.
**Criterios de Aceptación:**
- Drag and drop con RecyclerView
- Validación de orden correcto
- Sistema de puntuación dinámico
- Interfaz táctil intuitiva
- Dependencias con Writing

### 3.9 Sistema de Evaluación

#### RF-016: Quizzes de Evaluación
**Prioridad:** Crítica
**Descripción:** Sistema de evaluación integral del aprendizaje.
**Criterios de Aceptación:**
- Preguntas de opción múltiple
- Generación automática de preguntas
- Puntuación mínima del 70% para aprobar
- Desbloqueo automático de temas al aprobar
- Historial persistente de resultados

#### RF-017: Historial de Actividades
**Prioridad:** Media
**Descripción:** Almacenamiento y visualización de historial de actividades.
**Criterios de Aceptación:**
- Historial de quizzes completados
- Historial de prácticas de pronunciación
- Historial de actividades de reading
- Resultados detallados por actividad
- Persistencia local con SQLite

---

## 4. REQUERIMIENTOS NO FUNCIONALES

### 4.1 Rendimiento

#### RNF-001: Tiempo de Respuesta
**Prioridad:** Alta
**Descripción:** La aplicación debe responder dentro de tiempos específicos.
**Especificaciones:**
- Tiempo de inicio: < 3 segundos
- Tiempo de respuesta de actividades: < 1 segundo
- Tiempo de carga de recursos: < 2 segundos
- Tiempo de procesamiento de audio: < 5 segundos

#### RNF-002: Uso de Recursos
**Prioridad:** Alta
**Descripción:** Optimización del uso de memoria y procesamiento.
**Especificaciones:**
- Uso de RAM: < 150MB en dispositivos estándar
- Uso de almacenamiento: < 500MB
- Optimización de archivos multimedia
- Gestión eficiente de memoria

### 4.2 Compatibilidad

#### RNF-003: Versiones de Android
**Prioridad:** Crítica
**Descripción:** Compatibilidad con diferentes versiones de Android.
**Especificaciones:**
- Versión mínima: Android API 21 (Android 5.0)
- Versión objetivo: Android API 34
- Soporte para diferentes tamaños de pantalla
- Orientación portrait para actividades específicas

### 4.3 Usabilidad

#### RNF-004: Interfaz de Usuario
**Prioridad:** Alta
**Descripción:** Interfaz intuitiva y fácil de usar.
**Especificaciones:**
- Diseño responsive adaptable
- Navegación intuitiva con retroalimentación visual
- Animaciones suaves en transiciones (500ms)
- Efectos visuales claros para estados
- Colores institucionales (azul, blanco, gris)

#### RNF-005: Accesibilidad
**Prioridad:** Media
**Descripción:** Accesibilidad para usuarios con discapacidades.
**Especificaciones:**
- Soporte para lectores de pantalla
- Contraste adecuado en elementos visuales
- Tamaños de fuente configurables
- Navegación por teclado
- Indicadores visuales claros

### 4.4 Confiabilidad

#### RNF-006: Disponibilidad
**Prioridad:** Alta
**Descripción:** La aplicación debe estar disponible cuando sea necesaria.
**Especificaciones:**
- Disponibilidad: 99.5% durante horarios de uso
- Recuperación automática de errores
- Manejo de conexiones de red intermitentes
- Modo offline para funcionalidades básicas

#### RNF-007: Manejo de Errores
**Prioridad:** Alta
**Descripción:** Manejo robusto de errores y excepciones.
**Especificaciones:**
- Manejo de errores de red y conexión
- Fallback para videos cuando fallan
- Validación de datos de entrada
- Logs detallados para debugging
- Mensajes de error informativos

### 4.5 Seguridad

#### RNF-008: Autenticación y Autorización
**Prioridad:** Crítica
**Descripción:** Seguridad en la autenticación y autorización.
**Especificaciones:**
- Autenticación con Firebase Auth
- Encriptación de datos sensibles
- Validación de permisos de usuario
- Almacenamiento seguro de credenciales
- Protección contra inyección SQL

#### RNF-009: Protección de Datos
**Prioridad:** Alta
**Descripción:** Protección de datos personales y académicos.
**Especificaciones:**
- Encriptación de datos locales
- Manejo seguro de archivos temporales
- Validación de entrada de usuario
- Protección contra acceso no autorizado
- Cumplimiento con políticas de privacidad

### 4.6 Mantenibilidad

#### RNF-010: Arquitectura
**Prioridad:** Media
**Descripción:** Arquitectura mantenible y extensible.
**Especificaciones:**
- Arquitectura modular y bien estructurada
- Separación clara de responsabilidades
- Uso de patrones de diseño consistentes
- Código reutilizable entre módulos
- Documentación técnica actualizada

---

## 5. ESPECIFICACIONES TÉCNICAS

### 5.1 Plataforma y Tecnologías

#### Tecnologías Base:
- **Plataforma:** Android (Java/Kotlin)
- **IDE:** Android Studio
- **Base de Datos:** SQLite local + Firebase (opcional)
- **Autenticación:** Firebase Authentication
- **Reconocimiento de Voz:** Vosk (offline)
- **Síntesis de Voz:** Android Text-to-Speech

#### Librerías y Dependencias:
- **UI:** RecyclerView, ViewPager2, ItemTouchHelper
- **Multimedia:** VideoView, MediaPlayer
- **Animaciones:** AlphaAnimation, Property Animation
- **Red:** Firebase SDK, Retrofit (opcional)
- **Almacenamiento:** SharedPreferences, SQLite

### 5.2 Arquitectura del Sistema

#### Componentes Principales:
1. **Activities:** Puntos de entrada de la aplicación
2. **Services:** Servicios en segundo plano
3. **Database Helper:** Gestión de base de datos local
4. **Progression Helper:** Control de progreso del usuario
5. **Pronunciation Evaluator:** Evaluación de pronunciación
6. **Module Tracker:** Seguimiento de módulos visitados

#### Flujo de Datos:
```
Usuario → Activity → Business Logic → Database/SharedPreferences
                ↓
         UI Update ← Data Processing ← External Services (Firebase)
```

### 5.3 Base de Datos

#### Entidades Principales:
- **USUARIO:** Información del usuario
- **RESULTADO_PRONUNCIACION:** Resultados de ejercicios de pronunciación
- **RESULTADO_QUIZ:** Resultados de quizzes
- **PROGRESO_MODULO:** Progreso por módulo y tema
- **HISTORIAL_ACTIVIDAD:** Historial de todas las actividades

### 5.4 Permisos Requeridos

#### Permisos de Android:
- `INTERNET` - Conectividad de red
- `RECORD_AUDIO` - Grabación de audio
- `ACCESS_NETWORK_STATE` - Estado de red
- `ACCESS_WIFI_STATE` - Estado de WiFi
- `WRITE_EXTERNAL_STORAGE` - Almacenamiento (API ≤ 28)
- `READ_EXTERNAL_STORAGE` - Lectura (API ≤ 32)
- `MANAGE_EXTERNAL_STORAGE` - Gestión de almacenamiento (API ≥ 30)
- `MODIFY_AUDIO_SETTINGS` - Configuración de audio

---

## 6. CASOS DE USO

### 6.1 Casos de Uso Principales

#### CU-001: Registrar Usuario
**Actor:** Usuario nuevo
**Precondición:** Aplicación instalada
**Flujo Principal:**
1. Usuario abre la aplicación
2. Selecciona "Registrarse"
3. Completa formulario (nombre, email, contraseña)
4. Sistema valida datos
5. Sistema crea cuenta en Firebase
6. Usuario es redirigido al menú principal

#### CU-002: Iniciar Sesión
**Actor:** Usuario registrado
**Precondición:** Usuario con cuenta existente
**Flujo Principal:**
1. Usuario abre la aplicación
2. Ingresa email y contraseña
3. Sistema valida credenciales
4. Sistema inicia sesión
5. Usuario accede al menú principal

#### CU-003: Completar Actividad de Listening
**Actor:** Estudiante
**Precondición:** Usuario autenticado, tema desbloqueado
**Flujo Principal:**
1. Usuario selecciona módulo Listening
2. Elige tema disponible
3. Escucha audio
4. Responde ejercicios
5. Sistema evalúa respuestas
6. Sistema actualiza progreso
7. Usuario ve resultados

#### CU-004: Practicar Pronunciación
**Actor:** Estudiante
**Precondición:** Usuario autenticado, tema desbloqueado
**Flujo Principal:**
1. Usuario selecciona módulo Speaking
2. Elige tema de pronunciación
3. Escucha modelo de pronunciación
4. Graba su pronunciación
5. Sistema evalúa pronunciación
6. Usuario recibe feedback
7. Sistema almacena resultado

#### CU-005: Activar Modo Libre
**Actor:** Instructor
**Precondición:** Usuario con permisos de instructor
**Flujo Principal:**
1. Instructor accede a perfil
2. Activa toggle "Modo Libre"
3. Sistema desbloquea todos los módulos
4. Instructor puede navegar sin restricciones
5. Puede desactivar modo libre en cualquier momento

---

## 7. INTERFAZ DE USUARIO

### 7.1 Principios de Diseño
- **Consistencia:** Uso consistente de colores, tipografías y elementos
- **Simplicidad:** Interfaz limpia y fácil de navegar
- **Accesibilidad:** Diseño accesible para diferentes usuarios
- **Responsividad:** Adaptación a diferentes tamaños de pantalla

### 7.2 Paleta de Colores
- **Primario:** Azul institucional (#1E3A8A)
- **Secundario:** Blanco (#FFFFFF)
- **Acento:** Gris (#6B7280)
- **Éxito:** Verde (#10B981)
- **Error:** Rojo (#EF4444)
- **Advertencia:** Amarillo (#F59E0B)

### 7.3 Componentes de UI
- **Botones:** Estilo consistente con iconografía temática
- **Cards:** Contenedores para contenido relacionado
- **Navegación:** Bottom navigation y carousel principal
- **Formularios:** Campos de entrada claros y validados
- **Feedback:** Mensajes informativos y de error claros

---

## 8. PRUEBAS Y VALIDACIÓN

### 8.1 Estrategia de Pruebas
- **Pruebas Unitarias:** Validación de lógica de negocio
- **Pruebas de Integración:** Validación de componentes
- **Pruebas de UI:** Validación de interfaz de usuario
- **Pruebas de Rendimiento:** Validación de tiempos de respuesta
- **Pruebas de Usabilidad:** Validación con usuarios finales

### 8.2 Criterios de Aceptación
- Todos los requerimientos funcionales implementados
- Cumplimiento de requerimientos no funcionales
- Pruebas pasadas con éxito (≥95% cobertura)
- Validación por parte del cliente
- Documentación técnica completa

---

## 9. ENTREGABLES

### 9.1 Código Fuente
- Código fuente completo de la aplicación
- Documentación técnica del código
- Scripts de construcción y despliegue
- Configuración de Firebase

### 9.2 Documentación
- Manual de usuario
- Manual técnico de instalación
- Documentación de API (si aplica)
- Documento de arquitectura

### 9.3 Recursos
- Archivos multimedia (videos, audios)
- Iconos y recursos gráficos
- Modelos de IA para reconocimiento de voz
- Configuraciones de base de datos

---

## 10. CRONOGRAMA Y ENTREGAS

### 10.1 Fases del Proyecto
1. **Fase 1:** Análisis y Diseño (2 semanas)
2. **Fase 2:** Desarrollo Core (6 semanas)
3. **Fase 3:** Integración y Pruebas (3 semanas)
4. **Fase 4:** Despliegue y Validación (1 semana)

### 10.2 Hitos Principales
- **Hito 1:** Aprobación de requerimientos
- **Hito 2:** Prototipo funcional
- **Hito 3:** Versión beta para pruebas
- **Hito 4:** Versión final entregada

---

## 11. RIESGOS Y MITIGACIÓN

### 11.1 Riesgos Técnicos
- **Riesgo:** Problemas con reconocimiento de voz
  **Mitigación:** Pruebas extensivas con diferentes acentos
- **Riesgo:** Rendimiento en dispositivos antiguos
  **Mitigación:** Optimización de código y recursos

### 11.2 Riesgos de Proyecto
- **Riesgo:** Cambios en requerimientos
  **Mitigación:** Proceso de cambio controlado
- **Riesgo:** Disponibilidad de recursos
  **Mitigación:** Plan de contingencia definido

---

## 12. CONCLUSIONES

El documento de requerimientos especifica una aplicación educativa completa y robusta que cumple con las necesidades de la Armada Nacional de Colombia para el aprendizaje del inglés. La aplicación integra tecnologías modernas con metodologías pedagógicas probadas, proporcionando una experiencia de aprendizaje efectiva y adaptada a las necesidades militares.

La implementación de un sistema de progresión inteligente, evaluación automática, y modo libre para instructores, posiciona a ColSpeak Armada como una herramienta educativa de vanguardia que facilita el aprendizaje del inglés en un contexto institucional.

---

**Aprobaciones:**
- [ ] Cliente (Armada Nacional de Colombia)
- [ ] Equipo de Desarrollo
- [ ] Gerente de Proyecto
- [ ] Arquitecto de Software

**Firmas:**
- Cliente: _________________ Fecha: _______
- Desarrollador: _________________ Fecha: _______

