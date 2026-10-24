package com.iyaadam.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CustomOrderActivity extends AppCompatActivity {

    private static final String API_URL =
            "https://iyaadam.uhd.com.ng/api/create_custom_order.php";

    private static final String RESTAURANT_NAME =
            "Chicken and Co.";

    private static final String RESTAURANT_ADDRESS =
            "Ibadan Road, Ijebu Ode, Ogun State, Nigeria";

    private static final String WHATSAPP_NUMBER =
            "2347089364492";

    private static final int RECORD_AUDIO_REQUEST = 101;

    private EditText requestText;
    private TextView recordingStatus;
    private Button recordButton;
    private Button submitButton;

    private MediaRecorder mediaRecorder;
    private String audioPath;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_order);

        requestText = findViewById(R.id.custom_request_text);
        recordingStatus = findViewById(R.id.recording_status);
        recordButton = findViewById(R.id.record_button);
        submitButton = findViewById(R.id.submit_custom_order);

        findViewById(R.id.custom_back_btn)
                .setOnClickListener(v -> finish());

        // Show Chicken and Co. contact information if these views exist.
        TextView addressView = findViewById(R.id.restaurant_address);
        if (addressView != null) {
            addressView.setText(RESTAURANT_ADDRESS);
        }

        TextView nameView = findViewById(R.id.restaurant_name);
        if (nameView != null) {
            nameView.setText(RESTAURANT_NAME);
        }

        // Optional WhatsApp contact button, if present in this layout.
        Button whatsappButton = findViewById(R.id.whatsapp_btn);
        if (whatsappButton != null) {
            whatsappButton.setOnClickListener(v ->
                    openWhatsApp(
                            "Hello Chicken and Co.! " +
                            "I would like to make a custom order."
                    )
            );
        }

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        submitButton.setOnClickListener(v -> submitRequest());
    }

    private void startRecording() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST
            );
            return;
        }

        try {
            File directory = getExternalFilesDir(
                    Environment.DIRECTORY_MUSIC
            );

            if (directory == null) {
                Toast.makeText(
                        this,
                        "Unable to access audio storage",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!directory.exists() && !directory.mkdirs()) {
                Toast.makeText(
                        this,
                        "Unable to create audio storage",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            audioPath = new File(
                    directory,
                    "custom_order_" +
                            System.currentTimeMillis() +
                            ".m4a"
            ).getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );
            mediaRecorder.setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
            );
            mediaRecorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
            );
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(audioPath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;

            recordingStatus.setText(
                    "🔴 Recording... Tap again to stop"
            );
            recordButton.setText("⏹ Stop Recording");

        } catch (Exception e) {
            releaseRecorder();

            isRecording = false;
            audioPath = null;

            Toast.makeText(
                    this,
                    "Unable to start recording",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void stopRecording() {

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }

            isRecording = false;
            releaseRecorder();

            recordingStatus.setText("✅ Voice note recorded");
            recordButton.setText("🎤 Record Again");

        } catch (Exception e) {
            isRecording = false;
            releaseRecorder();

            if (audioPath != null) {
                File file = new File(audioPath);
                if (file.exists()) {
                    file.delete();
                }
            }

            audioPath = null;
            recordingStatus.setText("Recording failed");
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }
    }

    private void submitRequest() {

        if (isRecording) {
            stopRecording();
        }

        String text = requestText.getText()
                .toString()
                .trim();

        boolean hasVoice = audioPath != null
                && new File(audioPath).exists();

        if (text.isEmpty() && !hasVoice) {
            Toast.makeText(
                    this,
                    "Please enter your request or record a voice note",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Sending...");

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String boundary =
                        "----ChickenCoBoundary" +
                                System.currentTimeMillis();

                URL url = new URL(API_URL);
                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                DataOutputStream output =
                        new DataOutputStream(
                                connection.getOutputStream()
                        );

                // Preserve the existing customer profile integration.
                android.content.SharedPreferences prefs =
                        getSharedPreferences(
                                "IyaAdamProfile",
                                MODE_PRIVATE
                        );

                String customerName =
                        prefs.getString("name", "");

                String customerPhone =
                        prefs.getString("phone", "");

                writeFormField(
                        output, boundary, "customer_name", customerName
                );

                writeFormField(
                        output, boundary, "customer_phone", customerPhone
                );

                writeFormField(
                        output, boundary, "request_text", text
                );

                writeFormField(
                        output, boundary, "restaurant_name",
                        RESTAURANT_NAME
                );

                writeFormField(
                        output, boundary, "restaurant_address",
                        RESTAURANT_ADDRESS
                );

                writeFormField(
                        output, boundary, "whatsapp_number",
                        WHATSAPP_NUMBER
                );

                if (hasVoice) {
                    File file = new File(audioPath);

                    output.writeBytes("--" + boundary + "\r\n");
                    output.writeBytes(
                            "Content-Disposition: form-data; " +
                            "name=\"voice_file\"; filename=\"" +
                            file.getName() + "\"\r\n"
                    );
                    output.writeBytes(
                            "Content-Type: audio/mp4\r\n\r\n"
                    );

                    try (FileInputStream input =
                                 new FileInputStream(file)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }

                    output.writeBytes("\r\n");
                }

                output.writeBytes("--" + boundary + "--\r\n");
                output.flush();
                output.close();

                int responseCode = connection.getResponseCode();

                java.io.InputStream responseStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                if (responseStream == null) {
                    throw new Exception("Empty server response");
                }

                StringBuilder response = new StringBuilder();

                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(responseStream)
                             )) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                org.json.JSONObject json =
                        new org.json.JSONObject(response.toString());

                boolean success = json.optBoolean("success", false);

                String message = json.optString(
                        "message",
                        success
                                ? "Custom order submitted"
                                : "Submission failed"
                );

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(
                                CustomOrderActivity.this,
                                "Custom order sent successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                        if (audioPath != null) {
                            File file = new File(audioPath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }

                        finish();

                    } else {
                        submitButton.setEnabled(true);
                        submitButton.setText("Submit Custom Order");

                        Toast.makeText(
                                CustomOrderActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit Custom Order");

                    Toast.makeText(
                            CustomOrderActivity.this,
                            "Unable to send custom order. Check your internet connection.",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void writeFormField(
            DataOutputStream output,
            String boundary,
            String name,
            String value
    ) throws Exception {

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes(
                "Content-Disposition: form-data; name=\"" +
                        name + "\"\r\n\r\n"
        );
        output.writeBytes(value != null ? value : "");
        output.writeBytes("\r\n");
    }

    private void openWhatsApp(String message) {
        try {
            String url = "https://wa.me/" +
                    WHATSAPP_NUMBER +
                    "?text=" + Uri.encode(message);

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Unable to open WhatsApp. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == RECORD_AUDIO_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startRecording();

        } else {
            Toast.makeText(
                    this,
                    "Microphone permission is required for voice orders",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            try {
                if (mediaRecorder != null) {
                    mediaRecorder.stop();
                }
            } catch (Exception ignored) {
            }
        }

        releaseRecorder();
        super.onDestroy();
    }
}
