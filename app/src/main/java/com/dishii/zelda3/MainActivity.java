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
import android.view.View;

//This class is the main SDLActivity and just sets up a bunch of default files
public class MainActivity extends SDLActivity {

    private class VirtualGamepadView extends View {
        private Paint strokePaint;
        private Paint fillPaint;
        private Paint textPaint;

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
            if (toggleButton.pressed) canvas.drawCircle(toggleButton.x, toggleButton.y, toggleButton.rx, fillPaint);
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

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (buttons == null || toggleButton == null) return true;
            
            boolean[] nextState = new boolean[buttons.length];
            boolean toggleNextState = false;
            
            boolean analogActive = false;
            float currentFingerX = 0;
            float currentFingerY = 0;
            
            int pointerCount = event.getPointerCount();
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            int actionId = event.getPointerId(actionIndex);

            // Handle touch down for floating analog
            if (controlsVisible && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)) {
                float tx = event.getX(actionIndex);
                float ty = event.getY(actionIndex);
                // Left half of screen, below the L button
                if (tx < getWidth() / 2f && ty > getHeight() * 0.35f) {
                    if (analogPointerId == -1) {
                        analogPointerId = actionId;
                        currentDpadX = tx;
                        currentDpadY = ty;
                        invalidate();
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
                
                float x = event.getX(i);
                float y = event.getY(i);
                
                // Check toggleButton
                float tx = x - toggleButton.x;
                float ty = y - toggleButton.y;
                if (tx*tx + ty*ty <= (toggleButton.rx*1.8f)*(toggleButton.rx*1.8f)) {
                    toggleNextState = true;
                }
                
                if (controlsVisible) {
                    // Check Analog Stick
                    int id = event.getPointerId(i);
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
            
            if (toggleButton.pressed != toggleNextState) {
                toggleButton.pressed = toggleNextState;
                changed = true;
                if (toggleNextState) { // Trigger toggle on press down
                    controlsVisible = !controlsVisible;
                }
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
        File dir = new File(Environment.getExternalStorageDirectory(), "zelda");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayout != null) {
            VirtualGamepadView gamepad = new VirtualGamepadView(this);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mLayout.addView(gamepad, params);
        }

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

        // Check if external storage is available
        if (isExternalStorageWritable()) {
            // Get the root directory of the external storage
            File externalDir = getExternalFilesDir(null);

            if (externalDir != null) {

                // Create a file object for the config file
                File configFile = new File(externalDir, "zelda3.ini");

                File datNotice = new File(externalDir, "PLACE zelda3_assets.dat HERE");

                File saves_folder = new File(externalDir+ File.separator + "saves");

                File saves_ref_folder = new File(saves_folder + File.separator + "ref");

                // Check if the folder doesn't exist, then create it
                saves_folder.mkdirs();

                saves_ref_folder.mkdirs();


                //copy reference saves and config to external data dir so user can change if needed.

                try {
                    AssetCopyUtil.copyAssetsToExternal(this, "saves/ref", getExternalFilesDir(null).getAbsolutePath() + "/saves/ref");
                    if (configFile.createNewFile()) {
                        InputStream inputStream;
                        try {
                            inputStream = getAssets().open("zelda3.ini");  // Replace with your actual asset file name
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                        // Write configuration data to configFile
                        writeDataToFile(configFile,inputStream);
                    }

                    // Copia zelda3_assets.dat dos assets para /sdcard/zelda/ se ainda não existir
                    File assetsFile = new File(externalDir, "zelda3_assets.dat");
                    if (!assetsFile.exists()) {
                        Log.i("Zelda3", "Copiando zelda3_assets.dat para " + assetsFile.getAbsolutePath());
                        try {
                            InputStream assetIn = getAssets().open("zelda3_assets.dat");
                            writeDataToFile(assetsFile, assetIn);
                            Log.i("Zelda3", "zelda3_assets.dat copiado com sucesso.");
                        } catch (IOException e) {
                            Log.e("Zelda3", "Erro ao copiar zelda3_assets.dat: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.i("Zelda3", "zelda3_assets.dat ja existe em " + assetsFile.getAbsolutePath() + ", pulando copia.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void writeDataToFile(File file,InputStream inputStream) {
        try {
            // Copy the content from the asset InputStream to the target file
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Check if external storage is available and writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
