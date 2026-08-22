package com.dishii.zelda3;
import org.libsdl.app.SDLActivity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.Manifest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.graphics.RectF;

//This class is the main SDLActivity and just sets up a bunch of default files
public class MainActivity extends SDLActivity {

    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private class VirtualGamepadView extends View {
        private Paint strokePaint;
        private Paint fillPaint;
        private Paint textPaint;
        private Paint progressPaint;

        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private static final long LONG_PRESS_DURATION_MS = 200;
        private long togglePressStartTime = 0;
        private boolean isToggleLongPressTriggered = false;
        private int togglePointerId = -1;
        private final RectF toggleProgressRect = new RectF();

        private final Runnable longPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (toggleButton != null && toggleButton.pressed) {
                    isToggleLongPressTriggered = true;
                    toggleButton.pressed = false;
                    togglePointerId = -1;
                    invalidate();

                    try {
                        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null && v.hasVibrator()) {
                            v.vibrate(60);
                        }
                    } catch (Exception ignored) {}

                    Intent intent = new Intent(getContext(), ConfigActivity.class);
                    getContext().startActivity(intent);
                }
            }
        };

        public VirtualGamepadView(Context context) {
            super(context);
            
            int colorWhite = 0xAAFFFFFF; // Semi-transparent white
            
            strokePaint = new Paint();
            strokePaint.setColor(colorWhite);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(6f);
            strokePaint.setAntiAlias(true);
            
            fillPaint = new Paint();
            fillPaint.setColor(colorWhite);
            fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            fillPaint.setAntiAlias(true);
            
            textPaint = new Paint();
            textPaint.setColor(colorWhite);
            textPaint.setTextSize(40f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
            textPaint.setAntiAlias(true);

            progressPaint = new Paint();
            progressPaint.setColor(0xFFFFD700); // Gold progress ring
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeWidth(8f);
            progressPaint.setAntiAlias(true);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        class ButtonDef {
            String label;
            float x, y, rx, ry; 
            int type; // 0 = circle, 1 = rounded rect
            int keycode;
            boolean pressed = false;
            ButtonDef(String label, float x, float y, float r, int keycode) {
                this.label = label; this.x = x; this.y = y; this.rx = r; this.ry = r; this.type = 0; this.keycode = keycode;
            }
            ButtonDef(String label, float x, float y, float rx, float ry, int type, int keycode) {
                this.label = label; this.x = x; this.y = y; this.rx = rx; this.ry = ry; this.type = type; this.keycode = keycode;
            }
        }
        
        ButtonDef[] buttons = null;
        ButtonDef dpadUp, dpadDown, dpadLeft, dpadRight;
        float defaultDpadX, defaultDpadY, dpadR;
        float currentDpadX, currentDpadY;
        float analogFingerX = 0, analogFingerY = 0;
        boolean analogPressed = false;
        int analogPointerId = -1;
        
        ButtonDef toggleButton;
        boolean controlsVisible = true;

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float minDim = Math.min(w, h);
            float bw = minDim / 14f; // Button radius based on height
            if (bw > 90) bw = 90;

            textPaint.setTextSize(bw * 0.7f); // Scale text to fit button
            strokePaint.setStrokeWidth(bw * 0.08f);

            dpadR = bw * 1.6f; // Reduced size
            defaultDpadX = bw * 3.0f;
            defaultDpadY = h - bw * 3.0f;
            currentDpadX = defaultDpadX;
            currentDpadY = defaultDpadY;
            float spacing = bw * 1.5f;

            dpadUp = new ButtonDef("^", defaultDpadX, defaultDpadY - spacing, bw, KeyEvent.KEYCODE_DPAD_UP);
            dpadDown = new ButtonDef("v", defaultDpadX, defaultDpadY + spacing, bw, KeyEvent.KEYCODE_DPAD_DOWN);
            dpadLeft = new ButtonDef("<", defaultDpadX - spacing, defaultDpadY, bw, KeyEvent.KEYCODE_DPAD_LEFT);
            dpadRight = new ButtonDef(">", defaultDpadX + spacing, defaultDpadY, bw, KeyEvent.KEYCODE_DPAD_RIGHT);

            buttons = new ButtonDef[] {
                new ButtonDef("A", w - bw * 2.0f, h - bw * 3.5f, bw, KeyEvent.KEYCODE_X),
                new ButtonDef("B", w - bw * 4.0f, h - bw * 1.5f, bw, KeyEvent.KEYCODE_Z),
                new ButtonDef("X", w - bw * 4.0f, h - bw * 5.5f, bw, KeyEvent.KEYCODE_S),
                new ButtonDef("Y", w - bw * 6.0f, h - bw * 3.5f, bw, KeyEvent.KEYCODE_A),
                new ButtonDef("START", w / 2f + bw*2.2f, h - bw*1.5f, bw*1.4f, bw*0.55f, 1, KeyEvent.KEYCODE_ENTER),
                new ButtonDef("SELECT", w / 2f - bw*2.2f, h - bw*1.5f, bw*1.4f, bw*0.55f, 1, KeyEvent.KEYCODE_SHIFT_RIGHT),
                new ButtonDef("L", bw * 3f, bw * 1.2f, bw*2f, bw*0.7f, 1, KeyEvent.KEYCODE_C),
                new ButtonDef("R", w - bw * 3f, bw * 1.2f, bw*2f, bw*0.7f, 1, KeyEvent.KEYCODE_V),
                dpadUp, dpadDown, dpadLeft, dpadRight
            };
            
            toggleButton = new ButtonDef("H", w / 2f, bw * 1.2f, bw * 0.5f, 0);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (buttons == null || toggleButton == null) return;
            
            // Draw toggle button
            if (toggleButton.pressed) {
                canvas.drawCircle(toggleButton.x, toggleButton.y, toggleButton.rx, fillPaint);

                // Draw progress arc around the button
                long elapsed = SystemClock.uptimeMillis() - togglePressStartTime;
                float progress = Math.min(1.0f, (float) elapsed / (float) LONG_PRESS_DURATION_MS);
                float sweepAngle = progress * 360f;

                float ringPadding = toggleButton.rx * 0.4f;
                toggleProgressRect.set(
                        toggleButton.x - toggleButton.rx - ringPadding,
                        toggleButton.y - toggleButton.rx - ringPadding,
                        toggleButton.x + toggleButton.rx + ringPadding,
                        toggleButton.y + toggleButton.rx + ringPadding
                );

                progressPaint.setStrokeWidth(toggleButton.rx * 0.25f);
                canvas.drawArc(toggleProgressRect, -90f, sweepAngle, false, progressPaint);

                if (progress < 1.0f) {
                    postInvalidateDelayed(16);
                }
            }
            canvas.drawCircle(toggleButton.x, toggleButton.y, toggleButton.rx, strokePaint);
            float oldSizeToggle = textPaint.getTextSize();
            textPaint.setTextSize(oldSizeToggle * 0.6f);
            float toggleTextY = toggleButton.y - ((textPaint.descent() + textPaint.ascent()) / 2);
            canvas.drawText(toggleButton.label, toggleButton.x, toggleTextY, textPaint);
            textPaint.setTextSize(oldSizeToggle);
            
            if (!controlsVisible) return;
            
            // Draw Analog Stick (replacing D-Pad)
            canvas.drawCircle(currentDpadX, currentDpadY, dpadR, strokePaint);
            
            float analogX = currentDpadX + analogFingerX;
            float analogY = currentDpadY + analogFingerY;
            float distSq = analogFingerX * analogFingerX + analogFingerY * analogFingerY;
            float maxDist = dpadR * 0.5f; // knob can move up to half radius
            if (distSq > maxDist * maxDist) {
                float dist = (float) Math.sqrt(distSq);
                float scale = maxDist / dist;
                analogX = currentDpadX + analogFingerX * scale;
                analogY = currentDpadY + analogFingerY * scale;
            }
            
            Paint knobPaint = new Paint(fillPaint);
            knobPaint.setStyle(Paint.Style.FILL);
            knobPaint.setColor(0xAAFFFFFF);
            canvas.drawCircle(analogX, analogY, dpadR * 0.5f, knobPaint);
            
            for (ButtonDef b : buttons) {
                if (b == dpadUp || b == dpadDown || b == dpadLeft || b == dpadRight) continue;
                
                Paint currentPaint = b.pressed ? fillPaint : strokePaint;
                int currentTextColor = b.pressed ? 0xFF000000 : 0xAAFFFFFF;
                textPaint.setColor(currentTextColor);
                
                if (b.type == 0) {
                    canvas.drawCircle(b.x, b.y, b.rx, currentPaint);
                } else if (b.type == 1) {
                    android.graphics.RectF rect = new android.graphics.RectF(b.x - b.rx, b.y - b.ry, b.x + b.rx, b.y + b.ry);
                    canvas.drawRoundRect(rect, b.ry, b.ry, currentPaint);
                }
                
                if (b.type == 1) {
                    float oldSize = textPaint.getTextSize();
                    textPaint.setTextSize(oldSize * 0.6f);
                    float textY = b.y - ((textPaint.descent() + textPaint.ascent()) / 2);
                    canvas.drawText(b.label, b.x, textY, textPaint);
                    textPaint.setTextSize(oldSize);
                } else {
                    float textY = b.y - ((textPaint.descent() + textPaint.ascent()) / 2);
                    canvas.drawText(b.label, b.x, textY, textPaint);
                }
            }
        }

        private boolean isTouchOverButton(float x, float y) {
            if (toggleButton != null) {
                float tx = x - toggleButton.x;
                float ty = y - toggleButton.y;
                if (tx * tx + ty * ty <= (toggleButton.rx * 2.0f) * (toggleButton.rx * 2.0f)) {
                    return true;
                }
            }
            if (buttons != null) {
                for (int j = 0; j < 8; j++) {
                    ButtonDef b = buttons[j];
                    float bx = x - b.x;
                    float by = y - b.y;
                    if (b.type == 0) {
                        if (bx * bx + by * by <= (b.rx * 1.8f) * (b.rx * 1.8f)) {
                            return true;
                        }
                    } else if (b.type == 1) {
                        if (Math.abs(bx) <= b.rx * 1.5f && Math.abs(by) <= b.ry * 2.0f) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (buttons == null || toggleButton == null) return true;
            
            boolean[] nextState = new boolean[buttons.length];
            
            boolean analogActive = false;
            float currentFingerX = 0;
            float currentFingerY = 0;
            
            int pointerCount = event.getPointerCount();
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            int actionId = event.getPointerId(actionIndex);

            // Handle touch down on toggle button
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                float tx = event.getX(actionIndex);
                float ty = event.getY(actionIndex);
                float dtx = tx - toggleButton.x;
                float dty = ty - toggleButton.y;
                if (dtx * dtx + dty * dty <= (toggleButton.rx * 2.0f) * (toggleButton.rx * 2.0f)) {
                    if (togglePointerId == -1) {
                        togglePointerId = actionId;
                        toggleButton.pressed = true;
                        isToggleLongPressTriggered = false;
                        togglePressStartTime = SystemClock.uptimeMillis();
                        mHandler.removeCallbacks(longPressRunnable);
                        mHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION_MS);
                        invalidate();
                    }
                }
            }

            // Handle touch up / cancel on toggle button
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                if (actionId == togglePointerId) {
                    mHandler.removeCallbacks(longPressRunnable);
                    if (!isToggleLongPressTriggered && toggleButton.pressed) {
                        controlsVisible = !controlsVisible;
                    }
                    toggleButton.pressed = false;
                    togglePointerId = -1;
                    isToggleLongPressTriggered = false;
                    invalidate();
                }
            }

            // Handle movement on toggle button (cancel if dragged away)
            if (action == MotionEvent.ACTION_MOVE && togglePointerId != -1) {
                int pIdx = event.findPointerIndex(togglePointerId);
                if (pIdx != -1) {
                    float px = event.getX(pIdx);
                    float py = event.getY(pIdx);
                    float dpx = px - toggleButton.x;
                    float dpy = py - toggleButton.y;
                    if (dpx * dpx + dpy * dpy > (toggleButton.rx * 2.5f) * (toggleButton.rx * 2.5f)) {
                        mHandler.removeCallbacks(longPressRunnable);
                        toggleButton.pressed = false;
                        togglePointerId = -1;
                        isToggleLongPressTriggered = false;
                        invalidate();
                    }
                }
            }

            // Handle touch down for floating analog
            if (controlsVisible && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)) {
                if (actionId != togglePointerId) {
                    float tx = event.getX(actionIndex);
                    float ty = event.getY(actionIndex);
                    // Left half of screen, below the L button, and not over any button
                    if (tx < getWidth() / 2f && ty > getHeight() * 0.35f && !isTouchOverButton(tx, ty)) {
                        if (analogPointerId == -1) {
                            analogPointerId = actionId;
                            currentDpadX = tx;
                            currentDpadY = ty;
                            invalidate();
                        }
                    }
                }
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                if (actionId == analogPointerId) {
                    analogPointerId = -1;
                    currentDpadX = defaultDpadX;
                    currentDpadY = defaultDpadY;
                    invalidate();
                }
            }

            for (int i = 0; i < pointerCount; i++) {
                if (action == MotionEvent.ACTION_POINTER_UP ||
                    action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                    if (actionIndex == i) continue;
                }
                
                int id = event.getPointerId(i);
                if (id == togglePointerId) continue;
                
                float x = event.getX(i);
                float y = event.getY(i);
                
                if (controlsVisible) {
                    // Check Analog Stick
                    if (id == analogPointerId) {
                        float dx = x - currentDpadX;
                        float dy = y - currentDpadY;
                        analogActive = true;
                        currentFingerX = dx;
                        currentFingerY = dy;
                        if (dy < -dpadR*0.3f) nextState[8] = true; // UP
                        if (dy > dpadR*0.3f) nextState[9] = true; // DOWN
                        if (dx < -dpadR*0.3f) nextState[10] = true; // LEFT
                        if (dx > dpadR*0.3f) nextState[11] = true; // RIGHT
                    }
                    
                    // Check other buttons
                    for (int j=0; j<8; j++) {
                        ButtonDef b = buttons[j];
                        float bx = x - b.x;
                        float by = y - b.y;
                        if (b.type == 0) {
                            if (bx*bx + by*by <= (b.rx*1.8f)*(b.rx*1.8f)) {
                                nextState[j] = true;
                            }
                        } else if (b.type == 1) {
                            if (Math.abs(bx) <= b.rx * 1.5f && Math.abs(by) <= b.ry * 2.0f) {
                                nextState[j] = true;
                            }
                        }
                    }
                }
            }
            
            boolean changed = false;
            
            if (analogActive) {
                if (analogFingerX != currentFingerX || analogFingerY != currentFingerY || !analogPressed) changed = true;
                analogFingerX = currentFingerX;
                analogFingerY = currentFingerY;
                analogPressed = true;
            } else {
                if (analogPressed) changed = true;
                analogFingerX = 0;
                analogFingerY = 0;
                analogPressed = false;
            }
            
            for (int i=0; i<buttons.length; i++) {
                if (buttons[i].pressed != nextState[i]) {
                    buttons[i].pressed = nextState[i];
                    changed = true;
                    if (nextState[i]) SDLActivity.onNativeKeyDown(buttons[i].keycode);
                    else SDLActivity.onNativeKeyUp(buttons[i].keycode);
                }
            }
            
            if (changed) invalidate();
            
            return true;
        }
    }

    @Override
    public File getExternalFilesDir(String type) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "zelda");
            if (dir.exists() || dir.mkdirs()) {
                return dir;
            }
        } catch (Exception ignored) {}
        return super.getExternalFilesDir(type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        if (mLayout != null) {
            VirtualGamepadView gamepad = new VirtualGamepadView(this);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mLayout.addView(gamepad, params);
        }

        if (!hasStoragePermission()) {
            requestStoragePermission();
        } else {
            ensureAssetsExtracted();
        }
    }

    public static native void nativeReloadConfig();
    public static native byte[] nativeGetInventory();
    public static native void nativeSetInventory(byte[] data);

    public static void reloadGameConfig() {
        try {
            nativeReloadConfig();
        } catch (UnsatisfiedLinkError | Exception e) {
            Log.e("Zelda3", "Erro ao chamar nativeReloadConfig: " + e.getMessage());
        }
    }

    public static byte[] getGameInventory() {
        try {
            return nativeGetInventory();
        } catch (UnsatisfiedLinkError | Exception e) {
            Log.e("Zelda3", "Erro ao chamar nativeGetInventory: " + e.getMessage());
            return null;
        }
    }

    public static void setGameInventory(byte[] data) {
        try {
            nativeSetInventory(data);
        } catch (UnsatisfiedLinkError | Exception e) {
            Log.e("Zelda3", "Erro ao chamar nativeSetInventory: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            ensureAssetsExtracted();
        }
        reloadGameConfig();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasStoragePermission()) {
            ensureAssetsExtracted();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void ensureAssetsExtracted() {
        if (!hasStoragePermission() || !isExternalStorageWritable()) {
            Log.w("Zelda3", "Sem permissao de armazenamento ou armazenamento indisponivel para copiar assets.");
            return;
        }

        File externalDir = getExternalFilesDir(null);
        if (externalDir == null) return;

        File configFile = new File(externalDir, "zelda3.ini");
        File saves_folder = new File(externalDir + File.separator + "saves");
        File saves_ref_folder = new File(saves_folder + File.separator + "ref");
        File shaders_folder = new File(externalDir + File.separator + "shaders");

        if (!saves_folder.exists()) saves_folder.mkdirs();
        if (!saves_ref_folder.exists()) saves_ref_folder.mkdirs();
        if (!shaders_folder.exists()) shaders_folder.mkdirs();

        try {
            AssetCopyUtil.copyAssetsToExternal(this, "saves/ref", saves_ref_folder.getAbsolutePath());
            AssetCopyUtil.copyAssetsToExternal(this, "shaders", shaders_folder.getAbsolutePath());

            if (!configFile.exists() || configFile.length() == 0) {
                try (InputStream inputStream = getAssets().open("zelda3.ini")) {
                    writeDataToFile(configFile, inputStream);
                } catch (IOException e) {
                    Log.e("Zelda3", "Erro ao copiar zelda3.ini: " + e.getMessage());
                }
            }

            File assetsFile = new File(externalDir, "zelda3_assets.dat");
            try (InputStream assetIn = getAssets().open("zelda3_assets.dat")) {
                int expectedSize = assetIn.available();
                if (!assetsFile.exists() || assetsFile.length() != expectedSize) {
                    Log.i("Zelda3", "Copiando zelda3_assets.dat para " + assetsFile.getAbsolutePath() + " (esperado: " + expectedSize + " bytes, atual: " + assetsFile.length() + " bytes)");
                    writeDataToFile(assetsFile, assetIn);
                    Log.i("Zelda3", "zelda3_assets.dat copiado com sucesso. Tamanho: " + assetsFile.length());
                } else {
                    Log.i("Zelda3", "zelda3_assets.dat valido ja existe em " + assetsFile.getAbsolutePath() + ", pulando copia.");
                }
            } catch (IOException e) {
                Log.e("Zelda3", "Erro ao copiar zelda3_assets.dat: " + e.getMessage());
                if (assetsFile.exists() && assetsFile.length() != 683888L) {
                    assetsFile.delete();
                }
            }

        } catch (IOException e) {
            Log.e("Zelda3", "Erro ao copiar assets: " + e.getMessage());
        }
    }

    private void writeDataToFile(File file, InputStream inputStream) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        } catch (IOException e) {
            Log.e("Zelda3", "Falha ao escrever no arquivo " + file.getAbsolutePath() + ": " + e.getMessage());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    // Check if external storage is available and writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
