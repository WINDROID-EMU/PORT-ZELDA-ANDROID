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
            
            int colorYellow = 0xFFF0D030; // Golden yellow
            
            strokePaint = new Paint();
            strokePaint.setColor(colorYellow);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(6f);
            strokePaint.setAntiAlias(true);
            
            fillPaint = new Paint();
            fillPaint.setColor(0x66F0D030); // Semi-transparent yellow
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAntiAlias(true);
            
            textPaint = new Paint();
            textPaint.setColor(colorYellow);
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
        float dpadX, dpadY, dpadR;
        
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

            dpadR = bw * 2.5f;
            dpadX = dpadR + bw * 0.5f;
            dpadY = h - dpadR - bw * 0.5f;
            float spacing = bw * 1.5f;

            dpadUp = new ButtonDef("^", dpadX, dpadY - spacing, bw, KeyEvent.KEYCODE_DPAD_UP);
            dpadDown = new ButtonDef("v", dpadX, dpadY + spacing, bw, KeyEvent.KEYCODE_DPAD_DOWN);
            dpadLeft = new ButtonDef("<", dpadX - spacing, dpadY, bw, KeyEvent.KEYCODE_DPAD_LEFT);
            dpadRight = new ButtonDef(">", dpadX + spacing, dpadY, bw, KeyEvent.KEYCODE_DPAD_RIGHT);

            buttons = new ButtonDef[] {
                new ButtonDef("A", w - bw * 2.0f, h - bw * 3.5f, bw, KeyEvent.KEYCODE_X),
                new ButtonDef("B", w - bw * 4.0f, h - bw * 2.0f, bw, KeyEvent.KEYCODE_Z),
                new ButtonDef("X", w - bw * 4.0f, h - bw * 5.0f, bw, KeyEvent.KEYCODE_S),
                new ButtonDef("Y", w - bw * 6.0f, h - bw * 3.5f, bw, KeyEvent.KEYCODE_A),
                new ButtonDef("START", w / 2f + bw*2.2f, h - bw*0.5f, bw*1.8f, bw*0.7f, 1, KeyEvent.KEYCODE_ENTER),
                new ButtonDef("SELECT", w / 2f - bw*2.2f, h - bw*0.5f, bw*1.8f, bw*0.7f, 1, KeyEvent.KEYCODE_SHIFT_RIGHT),
                new ButtonDef("L", bw * 3f, bw * 1.2f, bw*2f, bw*0.7f, 1, KeyEvent.KEYCODE_C),
                new ButtonDef("R", w - bw * 3f, bw * 1.2f, bw*2f, bw*0.7f, 1, KeyEvent.KEYCODE_V),
                dpadUp, dpadDown, dpadLeft, dpadRight
            };
            
            toggleButton = new ButtonDef("👁", w / 2f, bw * 1.2f, bw * 0.5f, 0);
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
            
            // Draw D-Pad cross
            android.graphics.Path cross = new android.graphics.Path();
            float bw = dpadUp.rx;
            float halfW = bw * 0.85f;
            float armL = dpadR;
            float r = halfW * 0.3f; // corner radius
            
            // Start at top-left of the UP arm
            cross.moveTo(dpadX - halfW, dpadY - armL + r);
            cross.quadTo(dpadX - halfW, dpadY - armL, dpadX - halfW + r, dpadY - armL);
            cross.lineTo(dpadX + halfW - r, dpadY - armL);
            cross.quadTo(dpadX + halfW, dpadY - armL, dpadX + halfW, dpadY - armL + r);
            cross.lineTo(dpadX + halfW, dpadY - halfW);
            // Right arm
            cross.lineTo(dpadX + armL - r, dpadY - halfW);
            cross.quadTo(dpadX + armL, dpadY - halfW, dpadX + armL, dpadY - halfW + r);
            cross.lineTo(dpadX + armL, dpadY + halfW - r);
            cross.quadTo(dpadX + armL, dpadY + halfW, dpadX + armL - r, dpadY + halfW);
            cross.lineTo(dpadX + halfW, dpadY + halfW);
            // Down arm
            cross.lineTo(dpadX + halfW, dpadY + armL - r);
            cross.quadTo(dpadX + halfW, dpadY + armL, dpadX + halfW - r, dpadY + armL);
            cross.lineTo(dpadX - halfW + r, dpadY + armL);
            cross.quadTo(dpadX - halfW, dpadY + armL, dpadX - halfW, dpadY + armL - r);
            cross.lineTo(dpadX - halfW, dpadY + halfW);
            // Left arm
            cross.lineTo(dpadX - armL + r, dpadY + halfW);
            cross.quadTo(dpadX - armL, dpadY + halfW, dpadX - armL, dpadY + halfW - r);
            cross.lineTo(dpadX - armL, dpadY - halfW + r);
            cross.quadTo(dpadX - armL, dpadY - halfW, dpadX - armL + r, dpadY - halfW);
            cross.lineTo(dpadX - halfW, dpadY - halfW);
            cross.close();
            
            canvas.drawPath(cross, strokePaint);
            
            // Draw pressed state for D-Pad arms
            if (dpadUp.pressed) canvas.drawRoundRect(new android.graphics.RectF(dpadX - halfW, dpadY - armL, dpadX + halfW, dpadY), r, r, fillPaint);
            if (dpadDown.pressed) canvas.drawRoundRect(new android.graphics.RectF(dpadX - halfW, dpadY, dpadX + halfW, dpadY + armL), r, r, fillPaint);
            if (dpadLeft.pressed) canvas.drawRoundRect(new android.graphics.RectF(dpadX - armL, dpadY - halfW, dpadX, dpadY + halfW), r, r, fillPaint);
            if (dpadRight.pressed) canvas.drawRoundRect(new android.graphics.RectF(dpadX, dpadY - halfW, dpadX + armL, dpadY + halfW), r, r, fillPaint);
            
            Paint solidPaint = new Paint();
            solidPaint.setColor(strokePaint.getColor());
            solidPaint.setStyle(Paint.Style.FILL);
            solidPaint.setAntiAlias(true);
            
            // Draw center circle
            canvas.drawCircle(dpadX, dpadY, halfW * 0.45f, solidPaint);
            
            // Draw arrows
            float arrowOffset = armL * 0.7f;
            float arrowSize = halfW * 0.4f;
            
            android.graphics.Path upArrow = new android.graphics.Path();
            upArrow.moveTo(dpadX, dpadY - arrowOffset - arrowSize);
            upArrow.lineTo(dpadX - arrowSize, dpadY - arrowOffset + arrowSize);
            upArrow.lineTo(dpadX + arrowSize, dpadY - arrowOffset + arrowSize);
            upArrow.close();
            canvas.drawPath(upArrow, solidPaint);
            
            android.graphics.Path downArrow = new android.graphics.Path();
            downArrow.moveTo(dpadX, dpadY + arrowOffset + arrowSize);
            downArrow.lineTo(dpadX - arrowSize, dpadY + arrowOffset - arrowSize);
            downArrow.lineTo(dpadX + arrowSize, dpadY + arrowOffset - arrowSize);
            downArrow.close();
            canvas.drawPath(downArrow, solidPaint);
            
            android.graphics.Path leftArrow = new android.graphics.Path();
            leftArrow.moveTo(dpadX - arrowOffset - arrowSize, dpadY);
            leftArrow.lineTo(dpadX - arrowOffset + arrowSize, dpadY - arrowSize);
            leftArrow.lineTo(dpadX - arrowOffset + arrowSize, dpadY + arrowSize);
            leftArrow.close();
            canvas.drawPath(leftArrow, solidPaint);
            
            android.graphics.Path rightArrow = new android.graphics.Path();
            rightArrow.moveTo(dpadX + arrowOffset + arrowSize, dpadY);
            rightArrow.lineTo(dpadX + arrowOffset - arrowSize, dpadY - arrowSize);
            rightArrow.lineTo(dpadX + arrowOffset - arrowSize, dpadY + arrowSize);
            rightArrow.close();
            canvas.drawPath(rightArrow, solidPaint);
            
            for (ButtonDef b : buttons) {
                if (b == dpadUp || b == dpadDown || b == dpadLeft || b == dpadRight) continue;
                
                if (b.type == 0) {
                    if (b.pressed) canvas.drawCircle(b.x, b.y, b.rx, fillPaint);
                    canvas.drawCircle(b.x, b.y, b.rx, strokePaint);
                } else if (b.type == 1) {
                    android.graphics.RectF rect = new android.graphics.RectF(b.x - b.rx, b.y - b.ry, b.x + b.rx, b.y + b.ry);
                    if (b.pressed) canvas.drawRoundRect(rect, b.ry, b.ry, fillPaint);
                    canvas.drawRoundRect(rect, b.ry, b.ry, strokePaint);
                }
                
                float textY = b.y - ((textPaint.descent() + textPaint.ascent()) / 2);
                if (b.type == 1) {
                    float oldSize = textPaint.getTextSize();
                    textPaint.setTextSize(oldSize * 0.6f);
                    canvas.drawText(b.label, b.x, b.y - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);
                    textPaint.setTextSize(oldSize);
                } else {
                    canvas.drawText(b.label, b.x, textY, textPaint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (buttons == null || toggleButton == null) return true;
            
            boolean[] nextState = new boolean[buttons.length];
            boolean toggleNextState = false;
            
            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    if (event.getActionIndex() == i) continue;
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
                    // Check D-Pad
                    float dx = x - dpadX;
                    float dy = y - dpadY;
                    if (dx*dx + dy*dy <= dpadR*dpadR) {
                        if (dy < -dpadR*0.2f) nextState[8] = true; // UP
                        if (dy > dpadR*0.2f) nextState[9] = true; // DOWN
                        if (dx < -dpadR*0.2f) nextState[10] = true; // LEFT
                        if (dx > dpadR*0.2f) nextState[11] = true; // RIGHT
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
                    datNotice.createNewFile();
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
