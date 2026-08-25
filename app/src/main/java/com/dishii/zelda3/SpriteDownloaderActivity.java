package com.dishii.zelda3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SpriteDownloaderActivity extends Activity {

    private static final String SPRITES_ASSETS_PATH = "sprites-gfx/snes/zelda3/link/sheets/";
    
    private ProgressBar progressBar;
    private TextView tvLoading;
    private LinearLayout gridSprites;
    private Button btnClose;

    // Shared bounded thread pool — max 3 concurrent image loads, queued FIFO
    private final ExecutorService imageLoadExecutor = new ThreadPoolExecutor(
            3, 3,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<SpriteItem> spriteItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_sprite_downloader);

        initViews();
        loadSpritesFromAssets();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_bar);
        tvLoading = findViewById(R.id.tv_loading);
        gridSprites = findViewById(R.id.grid_sprites);
        btnClose = findViewById(R.id.btn_close);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadSpritesFromAssets() {
        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        gridSprites.setVisibility(View.GONE);

        // Use the shared pool — asset listing is fast, no network involved
        imageLoadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<SpriteItem> sprites = new ArrayList<>();
                try {
                    String[] assetFiles = getAssets().list(SPRITES_ASSETS_PATH);
                    if (assetFiles != null) {
                        for (String filename : assetFiles) {
                            if (filename.endsWith(".zspr")) {
                                String displayName = filename.replace(".zspr", "").replace("_", " ");
                                sprites.add(new SpriteItem(filename, displayName));
                            }
                        }
                    }
                    Collections.sort(sprites, (a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final List<SpriteItem> finalSprites = sprites;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        tvLoading.setVisibility(View.GONE);

                        if (!finalSprites.isEmpty()) {
                            spriteItems = finalSprites;
                            displaySprites();
                        } else {
                            Toast.makeText(SpriteDownloaderActivity.this, "Nenhum sprite encontrado nos assets.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void displaySprites() {
        gridSprites.removeAllViews();
        gridSprites.setVisibility(View.VISIBLE);

        // Create rows of 4 sprites each
        int columns = 4;
        android.widget.LinearLayout currentRow = null;
        
        for (int i = 0; i < spriteItems.size(); i++) {
            if (i % columns == 0) {
                // Start a new row
                currentRow = new android.widget.LinearLayout(this);
                currentRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                currentRow.setGravity(android.view.Gravity.CENTER);
                gridSprites.addView(currentRow);
            }
            
            final SpriteItem sprite = spriteItems.get(i);
            View spriteView = createSpriteView(sprite);
            
            // Set equal width for each sprite in the row
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            params.setMargins(8, 8, 8, 8);
            spriteView.setLayoutParams(params);
            
            currentRow.addView(spriteView);
        }
    }

    private View createSpriteView(final SpriteItem sprite) {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(8, 8, 8, 8);
        container.setBackgroundColor(0xFF2a2a3e);
        container.setGravity(android.view.Gravity.CENTER);
        
        // Use LinearLayout.LayoutParams for API 16 compatibility
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(params);

        // Preview image - larger size
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(180, 180));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(0xFF1a1a2e);
        imageView.setPadding(8, 8, 8, 8);

        // Select button
        Button selectButton = new Button(this);
        selectButton.setText("Selecionar");
        selectButton.setTextSize(11);
        selectButton.setPadding(12, 8, 12, 8);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSprite(sprite);
            }
        });

        container.addView(imageView);
        container.addView(selectButton);

        // Load preview image from assets
        loadPreviewImageFromAssets(sprite.name, imageView);

        return container;
    }

    private void loadPreviewImageFromAssets(final String spriteName, final ImageView imageView) {
        // Tag the ImageView so stale results from recycled views are discarded
        imageView.setTag(spriteName);

        // Submit to the shared pool
        imageLoadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                final String thumbFilename = spriteName.replace(".zspr", ".png");
                final String thumbAssetPath = "sprites-gfx/snes/zelda3/link/sheets/thumbs/" + thumbFilename;

                // 1ª tentativa: miniatura já embutida nos assets do APK (funciona offline)
                try {
                    InputStream input = getAssets().open(thumbAssetPath);
                    bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                } catch (Exception assetEx) {
                    // Asset não encontrado — tenta internet como fallback
                }

                // 2ª tentativa: GitHub Pages (requer internet)
                if (bitmap == null) {
                    try {
                        String thumbUrl = "https://snesrev.github.io/sprites-gfx/snes/zelda3/link/sheets/thumbs/" + thumbFilename;
                        java.net.URL url = new java.net.URL(thumbUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        bitmap = BitmapFactory.decodeStream(input);
                        input.close();
                        connection.disconnect();
                    } catch (Exception e) {
                        // GitHub Pages falhou — tenta raw GitHub
                    }
                }

                // 3ª tentativa: raw GitHub
                if (bitmap == null) {
                    try {
                        String thumbUrl = "https://raw.githubusercontent.com/snesrev/sprites-gfx/testing/snes/zelda3/link/sheets/thumbs/" + thumbFilename;
                        java.net.URL url = new java.net.URL(thumbUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        bitmap = BitmapFactory.decodeStream(input);
                        input.close();
                        connection.disconnect();
                    } catch (Exception ex) {
                        bitmap = null;
                    }
                }

                final Bitmap finalBitmap = bitmap;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Only apply if the ImageView still belongs to this sprite
                        if (spriteName.equals(imageView.getTag())) {
                            if (finalBitmap != null) {
                                imageView.setImageBitmap(finalBitmap);
                            } else {
                                // Placeholder se nenhuma fonte funcionou
                                imageView.setBackgroundColor(0xFF3a3a4e);
                                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                            }
                        }
                    }
                });
            }
        });
    }

    private void selectSprite(final SpriteItem sprite) {
        new AlertDialog.Builder(this)
                .setTitle("Selecionar Sprite")
                .setMessage("Deseja selecionar \"" + sprite.displayName + "\" como sprite do Link?")
                .setPositiveButton("Sim", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        updateConfigWithSprite(sprite.name);
                        
                        Toast.makeText(SpriteDownloaderActivity.this, 
                                "Sprite \"" + sprite.displayName + "\" selecionado com sucesso!", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Close the activity after a short delay
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1500);
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void updateConfigWithSprite(String spriteName) {
        try {
            // Resolve the same root dir that MainActivity uses: /sdcard/zelda/
            File zeldaRootDir = getZeldaRootDir();
            if (zeldaRootDir == null) {
                Toast.makeText(this, "Armazenamento externo indisponível.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Copy sprite to the correct location the game engine will find
            copySpriteToZeldaDir(spriteName, zeldaRootDir);

            // Relative path from the zelda root — this is what zelda3.ini expects
            String spritePath = "sprites-gfx/snes/zelda3/link/sheets/" + spriteName;

            android.util.Log.d("SpriteDownloader", "Setting sprite path: " + spritePath);
            android.util.Log.d("SpriteDownloader", "Zelda root dir: " + zeldaRootDir.getAbsolutePath());

            ZeldaConfigHelper configHelper = new ZeldaConfigHelper(this);
            configHelper.setValue("Graphics", "LinkGraphics", spritePath);
            boolean saved = configHelper.save();

            android.util.Log.d("SpriteDownloader", "Config saved: " + saved);

            // Verify
            String savedValue = configHelper.getValue("Graphics", "LinkGraphics", "");
            android.util.Log.d("SpriteDownloader", "Saved value in config: " + savedValue);

            // Signal the running game to reload its config
            MainActivity.reloadGameConfig();

            Toast.makeText(this, "Sprite configurado: " + spriteName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("SpriteDownloader", "Error configuring sprite", e);
            e.printStackTrace();
            Toast.makeText(this, "Erro ao configurar sprite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns the same zelda root directory that MainActivity.getExternalFilesDir() uses:
     * /sdcard/zelda/
     * This is critical — the game engine resolves sprite paths relative to this directory.
     */
    private File getZeldaRootDir() {
        try {
            File dir = new File(android.os.Environment.getExternalStorageDirectory(), "zelda");
            if (dir.exists() || dir.mkdirs()) {
                return dir;
            }
        } catch (Exception e) {
            android.util.Log.e("SpriteDownloader", "Could not resolve zelda root dir", e);
        }
        return null;
    }

    private void copySpriteToZeldaDir(String spriteName, File zeldaRootDir) throws IOException {
        File spritesDir = new File(zeldaRootDir, "sprites-gfx/snes/zelda3/link/sheets");
        if (!spritesDir.exists() && !spritesDir.mkdirs()) {
            throw new IOException("Não foi possível criar o diretório: " + spritesDir.getAbsolutePath());
        }

        File outputFile = new File(spritesDir, spriteName);

        // Copy sprite from APK assets
        InputStream input = getAssets().open(SPRITES_ASSETS_PATH + spriteName);
        FileOutputStream output = new FileOutputStream(outputFile);
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        } finally {
            output.close();
            input.close();
        }

        android.util.Log.d("SpriteDownloader", "Sprite copiado para: " + outputFile.getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the shared pool to avoid thread leaks when the Activity is destroyed
        imageLoadExecutor.shutdownNow();
    }

    private static class SpriteItem {
        String name;
        String displayName;

        SpriteItem(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }
}
