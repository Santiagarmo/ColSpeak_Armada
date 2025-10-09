package com.example.speak.vosk;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class VoskSpeechRecognizer {
    private static final String TAG = "VoskSpeechRecognizer";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 4096;

    private final Context context;
    private Model model;
    private Recognizer recognizer;
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private RecognitionListener listener;
    private SpeechService speechService;

    public interface RecognitionListener {
        void onResult(String text);
        void onError(String error);
        void onPartialResult(String text);
    }

    public VoskSpeechRecognizer(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void initialize(RecognitionListener listener) {
        this.listener = listener;
        executorService.execute(() -> {
            try {
                // Obtener la ruta del directorio de assets
                String modelPath = context.getFilesDir().getAbsolutePath() + "/vosk-model";
                Log.d(TAG, "Intentando inicializar modelo en: " + modelPath);
                
                // Verificar si el directorio existe
                File modelDir = new File(modelPath);
                if (!modelDir.exists()) {
                    boolean created = modelDir.mkdirs();
                    Log.d(TAG, "Directorio del modelo creado: " + created);
                }
                
                // Listar el contenido de assets para depuración
                String[] assetFiles = context.getAssets().list("model-en-us");
                if (assetFiles == null || assetFiles.length == 0) {
                    throw new IOException("No se encontraron archivos del modelo en assets");
                }
                Log.d(TAG, "Contenido de model-en-us: " + Arrays.toString(assetFiles));
                
                // Copiar el modelo desde assets si no existe
                if (!new File(modelPath + "/conf").exists()) {
                    Log.d(TAG, "Modelo no encontrado, copiando desde assets...");
                    copyModelFromAssets();
                }
                
                // Verificar que el modelo se copió correctamente
                if (!new File(modelPath + "/conf").exists()) {
                    throw new IOException("No se pudo copiar el modelo a la ubicación correcta");
                }
                
                // Listar el contenido del directorio del modelo para depuración
                listDirectoryContents(new File(modelPath), 0);
                
                // Inicializar el modelo Vosk
                Log.d(TAG, "Inicializando modelo Vosk...");
                Model model = new Model(modelPath);
                this.model = model;
                setupRecognizer();
                
                // Mostrar mensaje de inicialización como Toast después de 2 segundos
                mainHandler.postDelayed(() -> {
                    if (context != null) {
                        android.widget.Toast.makeText(context, 
                            "Modelo Vosk inicializado correctamente", 
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                }, 2000);
            } catch (Exception e) {
                String error = "Error al inicializar Vosk: " + e.getMessage();
                Log.e(TAG, error, e);
                notifyError(error);
            }
        });
    }

    private void copyModelFromAssets() throws IOException {
        String modelPath = context.getFilesDir().getAbsolutePath() + "/vosk-model";
        File modelDir = new File(modelPath);
        if (!modelDir.exists()) {
            boolean created = modelDir.mkdirs();
            Log.d(TAG, "Directorio del modelo creado: " + created);
        }

        // Copiar recursivamente todo el contenido del modelo
        copyDirectoryFromAssets("model-en-us", modelPath);
    }

    private void copyDirectoryFromAssets(String assetPath, String targetPath) throws IOException {
        String[] files = context.getAssets().list(assetPath);
        if (files != null) {
            for (String file : files) {
                String assetFilePath = assetPath + "/" + file;
                String targetFilePath = targetPath + "/" + file;
                
                // Verificar si es un directorio
                String[] subFiles = context.getAssets().list(assetFilePath);
                if (subFiles != null && subFiles.length > 0) {
                    // Es un directorio, crearlo y copiar su contenido
                    File targetDir = new File(targetFilePath);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        Log.d(TAG, "Directorio creado: " + targetFilePath + " - " + created);
                    }
                    copyDirectoryFromAssets(assetFilePath, targetFilePath);
                } else {
                    // Es un archivo, copiarlo
                    File targetFile = new File(targetFilePath);
                    if (!targetFile.exists()) {
                        Log.d(TAG, "Copiando archivo: " + assetFilePath);
                        InputStream in = context.getAssets().open(assetFilePath);
                        OutputStream out = new FileOutputStream(targetFile);
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        out.close();
                        Log.d(TAG, "Archivo copiado exitosamente: " + targetFilePath);
                    }
                }
            }
        }
    }

    private void setupRecognizer() {
        try {
            // Configurar el reconocedor con parámetros optimizados para inglés
            recognizer = new Recognizer(model, SAMPLE_RATE);
            
            // Configuración para mejorar la precisión en inglés y permitir textos largos
            recognizer.setMaxAlternatives(0); // No necesitamos alternativas para textos largos
            recognizer.setWords(true); // Reconocer palabras completas
            recognizer.setPartialWords(true); // Permitir reconocimiento parcial
            
            // Configurar parámetros de audio para mejor captura
            setupAudioRecord();
            
            Log.d(TAG, "Reconocedor configurado para textos largos en inglés");
        } catch (Exception e) {
            String error = "Error al configurar el reconocedor: " + e.getMessage();
            Log.e(TAG, error, e);
            notifyError(error);
        }
    }

    private void setupAudioRecord() {
        // Configurar parámetros de audio para mejor calidad
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2 // Buffer más grande para mejor captura
        );
    }

    public void startListening() {
        if (recognizer == null || audioRecord == null) {
            notifyError("Reconocedor no inicializado");
            return;
        }

        if (isListening) {
            return;
        }

        isListening = true;
        executorService.execute(() -> {
            try {
                // Inicializar el audio
                audioRecord.startRecording();
                byte[] buffer = new byte[BUFFER_SIZE];
                
                // Resetear el reconocedor para una nueva sesión
                recognizer.reset();
                
                // Notificar que estamos listos para escuchar
                notifyResult("Escuchando...");
                
                while (isListening) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        // Procesar el audio con el reconocedor
                        if (recognizer.acceptWaveForm(buffer, read)) {
                            String result = recognizer.getResult();
                            try {
                                JSONObject jsonResult = new JSONObject(result);
                                String text = jsonResult.getString("text");
                                if (!text.isEmpty()) {
                                    // Limpiar el texto de caracteres especiales y espacios extra
                                    text = text.trim().replaceAll("\\s+", " ");
                                    notifyResult(text);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error procesando resultado JSON", e);
                            }
                        } else {
                            String partial = recognizer.getPartialResult();
                            try {
                                JSONObject jsonPartial = new JSONObject(partial);
                                String text = jsonPartial.getString("partial");
                                if (!text.isEmpty()) {
                                    // Limpiar el texto parcial de caracteres especiales y espacios extra
                                    text = text.trim().replaceAll("\\s+", " ");
                                    notifyPartialResult(text);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error procesando resultado parcial JSON", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error durante el reconocimiento", e);
                notifyError("Error durante el reconocimiento: " + e.getMessage());
            } finally {
                stopListening();
            }
        });
    }

    public void stopListening() {
        isListening = false;
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    public void destroy() {
        stopListening();
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
        executorService.shutdown();
    }

    private void notifyResult(String result) {
        if (listener != null) {
            mainHandler.post(() -> listener.onResult(result));
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(error));
        }
    }

    private void notifyPartialResult(String partial) {
        if (listener != null) {
            mainHandler.post(() -> listener.onPartialResult(partial));
        }
    }

    private void listDirectoryContents(File dir, int depth) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    Log.d(TAG, "  ".repeat(depth) + (file.isDirectory() ? "[DIR] " : "[FILE] ") + file.getName());
                    if (file.isDirectory()) {
                        listDirectoryContents(file, depth + 1);
                    }
                }
            }
        }
    }
} 