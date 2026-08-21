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
        private Paint paint;
        private Paint textPaint;

        public VirtualGamepadView(Context context) {
            super(context);
            paint = new Paint();
            paint.setColor(0x88FFFFFF); // Semi-transparent white
            paint.setStyle(Paint.Style.FILL);
            textPaint = new Paint();
            textPaint.setColor(0xFF000000);
            textPaint.setTextSize(40f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
        }

        class ButtonDef {
            String label;
            float x, y, r;
            int keycode;
            boolean pressed = false;
            ButtonDef(String label, float x, float y, float r, int keycode) {
                this.label = label; this.x = x; this.y = y; this.r = r; this.keycode = keycode;
            }
        }
        
        ButtonDef[] buttons = null;
        ButtonDef dpadUp = new ButtonDef("U", 0,0,0, KeyEvent.KEYCODE_DPAD_UP);
        ButtonDef dpadDown = new ButtonDef("D", 0,0,0, KeyEvent.KEYCODE_DPAD_DOWN);
        ButtonDef dpadLeft = new ButtonDef("L", 0,0,0, KeyEvent.KEYCODE_DPAD_LEFT);
        ButtonDef dpadRight = new ButtonDef("R", 0,0,0, KeyEvent.KEYCODE_DPAD_RIGHT);
        
        float dpadX, dpadY, dpadR;

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float minDim = Math.min(w, h);
            float bw = minDim / 12f; // Button radius based on height
            if (bw > 90) bw = 90; // Cap the radius so they don't get huge

            textPaint.setTextSize(bw * 0.7f); // Scale text to fit button

            dpadR = bw * 1.8f;
            dpadX = dpadR + bw;
            dpadY = h - dpadR - bw;

            buttons = new ButtonDef[] {
                new ButtonDef("A", w - bw * 1.5f, h - bw * 2.8f, bw, KeyEvent.KEYCODE_X),
                new ButtonDef("B", w - bw * 3.2f, h - bw * 1.5f, bw, KeyEvent.KEYCODE_Z),
                new ButtonDef("X", w - bw * 3.2f, h - bw * 4.1f, bw, KeyEvent.KEYCODE_S),
                new ButtonDef("Y", w - bw * 4.9f, h - bw * 2.8f, bw, KeyEvent.KEYCODE_A),
                new ButtonDef("Start", w / 2f + bw*1.4f, h - bw*0.8f, bw*0.7f, KeyEvent.KEYCODE_ENTER),
                new ButtonDef("Select", w / 2f - bw*1.4f, h - bw*0.8f, bw*0.7f, KeyEvent.KEYCODE_SHIFT_RIGHT),
                new ButtonDef("L", bw * 2f, bw * 1.5f, bw, KeyEvent.KEYCODE_C),
                new ButtonDef("R", w - bw * 2f, bw * 1.5f, bw, KeyEvent.KEYCODE_V),
                dpadUp, dpadDown, dpadLeft, dpadRight
            };
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (buttons == null) return;
            
            // Draw D-Pad background
            paint.setColor(0x66FFFFFF);
            canvas.drawCircle(dpadX, dpadY, dpadR, paint);
            
            // Draw center dot for D-pad
            paint.setColor(0x88000000);
            canvas.drawCircle(dpadX, dpadY, dpadR*0.2f, paint);
            
            for (ButtonDef b : buttons) {
                if (b == dpadUp || b == dpadDown || b == dpadLeft || b == dpadRight) continue;
                if (b.pressed) paint.setColor(0xCCFFFFFF);
                else paint.setColor(0x66FFFFFF);
                canvas.drawCircle(b.x, b.y, b.r, paint);
                canvas.drawText(b.label, b.x, b.y - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (buttons == null) return true;
            
            boolean[] nextState = new boolean[buttons.length];
            
            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    if (event.getActionIndex() == i) continue;
                }
                
                float x = event.getX(i);
                float y = event.getY(i);
                
                // Check D-Pad
                float dx = x - dpadX;
                float dy = y - dpadY;
                if (dx*dx + dy*dy <= (dpadR*1.8f)*(dpadR*1.8f)) {
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
                    if (bx*bx + by*by <= (b.r*1.8f)*(b.r*1.8f)) {
                        nextState[j] = true;
                    }
                }
            }
            
            boolean changed = false;
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
