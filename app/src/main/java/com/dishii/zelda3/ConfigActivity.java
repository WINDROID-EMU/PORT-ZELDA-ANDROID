package com.dishii.zelda3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigActivity extends Activity {

    private ZeldaConfigHelper configHelper;

    // Tabs & Panels
    private Button tabGeneral, tabGraphics, tabSound, tabFeatures, tabRawIni;
    private View panelGeneral, panelGraphics, panelSound, panelFeatures, panelRawIni;
    private Button currentTabButton;
    private View currentPanel;

    // Header buttons
    private Button btnSave, btnClose, btnRestoreDefaults, btnReloadRaw;

    // Features & Item Editor
    private Button btnOpenItemEditor;

    // General controls
    private Spinner spAspectRatio, spLanguage;
    private CheckBox cbAutosave, cbDisableFrameDelay, cbDisplayPerf;

    // Graphics controls
    private Spinner spOutputMethod, spWindowScale, spShaderPreset;
    private CheckBox cbNewRenderer, cbEnhancedMode7, cbNoSpriteLimits, cbLinearFiltering, cbIgnoreAspectRatio, cbDimFlashes, cbEnableShader;
    private TextView tvShaderDesc;
    private View layoutCustomShader;
    private EditText etLinkGraphics, etShader;
    private boolean isUpdatingShaderUi = false;

    // Sound controls
    private CheckBox cbEnableAudio, cbResumeMsu;
    private Spinner spAudioFreq, spAudioSamples, spAudioChannels, spEnableMsu;
    private SeekBar sbMsuVolume;
    private TextView tvMsuVolumeLabel;

    // Features controls
    private CheckBox cbItemSwitchLr, cbItemSwitchLimit, cbTurnWhileDashing, cbMirrorDarkworld,
            cbCollectWithSword, cbBreakPots, cbDisableLowHealthBeep, cbSkipIntro,
            cbShowMaxYellow, cbMoreActiveBombs, cbCarryMoreRupees, cbMiscBugFixes,
            cbGameChangingBugFixes, cbCancelBirdTravel, cbSkipDialogueA, cbMaxHearts;

    // Raw INI
    private EditText etRawIni;

    public static class LanguageOption {
        public final String name;
        public final String code;

        public LanguageOption(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<LanguageOption> LANGUAGE_OPTIONS = Arrays.asList(
            new LanguageOption("Português do Brasil (pt)", "pt"),
            new LanguageOption("English / Original (us)", "us")
    );

    public static class ShaderPreset {
        public final String name;
        public final String path;
        public final String description;

        public ShaderPreset(String name, String path, String description) {
            this.name = name;
            this.path = path;
            this.description = description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<ShaderPreset> SHADER_PRESETS = Arrays.asList(
            new ShaderPreset("Nenhum (Pixel Art Original)", "", "Renderização nativa pixel art clássica sem pós-processamento adicional."),
            new ShaderPreset("Sharp Bilinear (Nitidez Perfeita / Anti-Shimmering)", "shaders/sharp_bilinear.glsl", "Nitidez máxima dos pixels sem distorções ou ondulações ao movimentar a câmera."),
            new ShaderPreset("Sharp Bilinear + Scanlines (Nítido com Scanlines)", "shaders/sharp_bilinear_scanlines.glsl", "Pixels nítidos combinados com linhas de varredura (scanlines) sutis estilo TV retrô."),
            new ShaderPreset("Advanced Sharpening (Nitidez Aprimorada / CAS)", "shaders/advanced_sharpening.glsl", "Filtro adaptativo de alta nitidez que realça bordas, texturas e detalhes dos sprites."),
            new ShaderPreset("Scale2x / AdvMame (Pixel Art Suavizado)", "shaders/scale2x.glsl", "Algoritmo inteligente que suaviza escadas de pixels em diagonais preservando contornos."),
            new ShaderPreset("HQ2x (Alta Qualidade Suave)", "shaders/hq2x.glsl", "Filtro avançado de interpolação e suavização 2x de alta fidelidade para pixel art."),
            new ShaderPreset("CRT-Easymode (TV Tubo Clássica / Scanlines)", "shaders/crt_easymode.glsl", "Simulação autêntica de monitor CRT retrô com scanlines, máscara de fósforo e contraste."),
            new ShaderPreset("Bicubic Sharpen (Filtro Bicúbico Nítido)", "shaders/bicubic_sharpen.glsl", "Interpolação bicúbica suave com realce de nitidez para resoluções modernas."),
            new ShaderPreset("LCD Grid (Matriz de LCD Portátil)", "shaders/lcd_grid.glsl", "Simula a matriz de grade de tela LCD retrô portátil com nitidez uniforme."),
            new ShaderPreset("Vibrant Enhancer (Cores Vivas & Alto Contraste)", "shaders/vibrant_enhancer.glsl", "Acentua o contraste, saturação e vivacidade das cores em telas OLED e IPS."),
            new ShaderPreset("Personalizado (.glsl customizado)", "__custom__", "Carrega um arquivo shader customizado do armazenamento interno ou externo.")
    );

    private static final List<String> ASPECT_RATIOS = Arrays.asList(
            "18:9", "16:9", "4:3", "16:10", "19.5:9", "20:9", "21:9",
            "18:9, extend_y", "16:9, extend_y", "4:3, extend_y"
    );

    private static final List<String> OUTPUT_METHODS = Arrays.asList("SDL", "OpenGL", "OpenGL ES", "SDL-Software");
    private static final List<String> WINDOW_SCALES = Arrays.asList("1", "2", "3", "4", "5");
    private static final List<String> AUDIO_FREQS = Arrays.asList("44100", "48000", "32000", "22050", "11025");
    private static final List<String> AUDIO_SAMPLES = Arrays.asList("512", "1024", "2048", "4096");
    private static final List<String> AUDIO_CHANNELS = Arrays.asList("2", "1");
    private static final List<String> MSU_OPTIONS = Arrays.asList("false", "true", "deluxe", "opuz", "deluxe-opuz");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_config);

        configHelper = new ZeldaConfigHelper(this);

        initViews();
        setupSpinners();
        loadValuesToUi();
        setupEvents();

        selectTab(tabGeneral, panelGeneral);
    }

    private void initViews() {
        tabGeneral = findViewById(R.id.tab_general);
        tabGraphics = findViewById(R.id.tab_graphics);
        tabSound = findViewById(R.id.tab_sound);
        tabFeatures = findViewById(R.id.tab_features);
        tabRawIni = findViewById(R.id.tab_raw_ini);

        panelGeneral = findViewById(R.id.panel_general);
        panelGraphics = findViewById(R.id.panel_graphics);
        panelSound = findViewById(R.id.panel_sound);
        panelFeatures = findViewById(R.id.panel_features);
        panelRawIni = findViewById(R.id.panel_raw_ini);

        btnSave = findViewById(R.id.btn_save);
        btnClose = findViewById(R.id.btn_close);
        btnRestoreDefaults = findViewById(R.id.btn_restore_defaults);
        btnReloadRaw = findViewById(R.id.btn_reload_raw);

        // General
        spAspectRatio = findViewById(R.id.sp_aspect_ratio);
        cbAutosave = findViewById(R.id.cb_autosave);
        cbDisableFrameDelay = findViewById(R.id.cb_disable_frame_delay);
        cbDisplayPerf = findViewById(R.id.cb_display_perf);
        spLanguage = findViewById(R.id.sp_language);

        // Graphics
        spOutputMethod = findViewById(R.id.sp_output_method);
        spWindowScale = findViewById(R.id.sp_window_scale);
        cbNewRenderer = findViewById(R.id.cb_new_renderer);
        cbEnhancedMode7 = findViewById(R.id.cb_enhanced_mode7);
        cbNoSpriteLimits = findViewById(R.id.cb_no_sprite_limits);
        cbLinearFiltering = findViewById(R.id.cb_linear_filtering);
        cbIgnoreAspectRatio = findViewById(R.id.cb_ignore_aspect_ratio);
        cbDimFlashes = findViewById(R.id.cb_dim_flashes);
        etLinkGraphics = findViewById(R.id.et_link_graphics);
        
        // Shaders & Filters
        cbEnableShader = findViewById(R.id.cb_enable_shader);
        spShaderPreset = findViewById(R.id.sp_shader_preset);
        tvShaderDesc = findViewById(R.id.tv_shader_desc);
        layoutCustomShader = findViewById(R.id.layout_custom_shader);
        etShader = findViewById(R.id.et_shader);

        // Sound
        cbEnableAudio = findViewById(R.id.cb_enable_audio);
        cbResumeMsu = findViewById(R.id.cb_resume_msu);
        spAudioFreq = findViewById(R.id.sp_audio_freq);
        spAudioSamples = findViewById(R.id.sp_audio_samples);
        spAudioChannels = findViewById(R.id.sp_audio_channels);
        spEnableMsu = findViewById(R.id.sp_enable_msu);
        sbMsuVolume = findViewById(R.id.sb_msu_volume);
        tvMsuVolumeLabel = findViewById(R.id.tv_msu_volume_label);

        // Features
        cbItemSwitchLr = findViewById(R.id.cb_item_switch_lr);
        cbItemSwitchLimit = findViewById(R.id.cb_item_switch_limit);
        cbTurnWhileDashing = findViewById(R.id.cb_turn_while_dashing);
        cbMirrorDarkworld = findViewById(R.id.cb_mirror_darkworld);
        cbCollectWithSword = findViewById(R.id.cb_collect_with_sword);
        cbBreakPots = findViewById(R.id.cb_break_pots);
        cbDisableLowHealthBeep = findViewById(R.id.cb_disable_low_health_beep);
        cbSkipIntro = findViewById(R.id.cb_skip_intro);
        cbShowMaxYellow = findViewById(R.id.cb_show_max_yellow);
        cbMoreActiveBombs = findViewById(R.id.cb_more_active_bombs);
        cbCarryMoreRupees = findViewById(R.id.cb_carry_more_rupees);
        cbMiscBugFixes = findViewById(R.id.cb_misc_bug_fixes);
        cbGameChangingBugFixes = findViewById(R.id.cb_game_changing_bug_fixes);
        cbCancelBirdTravel = findViewById(R.id.cb_cancel_bird_travel);
        cbSkipDialogueA = findViewById(R.id.cb_skip_dialogue_a);
        cbMaxHearts = findViewById(R.id.cb_max_hearts);
        btnOpenItemEditor = findViewById(R.id.btn_open_item_editor);

        // Raw
        etRawIni = findViewById(R.id.et_raw_ini);
    }

    private void setupSpinners() {
        setupSpinnerAdapter(spAspectRatio, ASPECT_RATIOS);
        setupSpinnerAdapter(spOutputMethod, OUTPUT_METHODS);
        setupSpinnerAdapter(spWindowScale, WINDOW_SCALES);
        setupSpinnerAdapter(spAudioFreq, AUDIO_FREQS);
        setupSpinnerAdapter(spAudioSamples, AUDIO_SAMPLES);
        setupSpinnerAdapter(spAudioChannels, AUDIO_CHANNELS);
        setupSpinnerAdapter(spEnableMsu, MSU_OPTIONS);

        List<String> langNames = new ArrayList<>();
        for (LanguageOption opt : LANGUAGE_OPTIONS) {
            langNames.add(opt.name);
        }
        setupSpinnerAdapter(spLanguage, langNames);

        List<String> presetNames = new ArrayList<>();
        for (ShaderPreset p : SHADER_PRESETS) {
            presetNames.add(p.name);
        }
        setupSpinnerAdapter(spShaderPreset, presetNames);
    }

    private void setupSpinnerAdapter(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSpinnerSelection(Spinner spinner, List<String> list, String value, String defVal) {
        if (value == null) value = defVal;
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(value.trim())) {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            spinner.setSelection(idx);
        } else if (defVal != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equalsIgnoreCase(defVal.trim())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void loadValuesToUi() {
        // General
        String aspect = configHelper.getValue("General", "ExtendedAspectRatio", "18:9");
        if (aspect != null && aspect.contains("#")) aspect = aspect.substring(0, aspect.indexOf('#')).trim();
        if (aspect != null && aspect.contains(";")) aspect = aspect.substring(0, aspect.indexOf(';')).trim();
        setSpinnerSelection(spAspectRatio, ASPECT_RATIOS, aspect, "18:9");
        cbAutosave.setChecked(configHelper.getBoolValue("General", "Autosave", false));
        cbDisableFrameDelay.setChecked(configHelper.getBoolValue("General", "DisableFrameDelay", false));
        cbDisplayPerf.setChecked(configHelper.getBoolValue("General", "DisplayPerfInTitle", false));
        String lang = configHelper.getValue("General", "Language", "pt");
        if (lang != null && lang.contains("#")) lang = lang.substring(0, lang.indexOf('#')).trim();
        if (lang != null && lang.contains(";")) lang = lang.substring(0, lang.indexOf(';')).trim();
        if (lang == null || lang.isEmpty() || lang.equalsIgnoreCase("pt") || lang.equalsIgnoreCase("pt-br") || lang.equalsIgnoreCase("pt_br")) {
            spLanguage.setSelection(0); // Português do Brasil (pt)
        } else {
            spLanguage.setSelection(1); // English / Original (us)
        }

        // Graphics
        String outputMethod = configHelper.getValue("Graphics", "OutputMethod", "SDL");
        if (outputMethod != null && outputMethod.contains("#")) outputMethod = outputMethod.substring(0, outputMethod.indexOf('#')).trim();
        setSpinnerSelection(spOutputMethod, OUTPUT_METHODS, outputMethod, "SDL");

        String winScale = String.valueOf(configHelper.getIntValue("Graphics", "WindowScale", 3));
        setSpinnerSelection(spWindowScale, WINDOW_SCALES, winScale, "3");

        cbNewRenderer.setChecked(configHelper.getBoolValue("Graphics", "NewRenderer", true));
        cbEnhancedMode7.setChecked(configHelper.getBoolValue("Graphics", "EnhancedMode7", true));
        cbNoSpriteLimits.setChecked(configHelper.getBoolValue("Graphics", "NoSpriteLimits", true));
        cbLinearFiltering.setChecked(configHelper.getBoolValue("Graphics", "LinearFiltering", false));
        cbIgnoreAspectRatio.setChecked(configHelper.getBoolValue("Graphics", "IgnoreAspectRatio", false));
        cbDimFlashes.setChecked(configHelper.getBoolValue("Graphics", "DimFlashes", false));

        etLinkGraphics.setText(configHelper.getValue("Graphics", "LinkGraphics", ""));

        // Shaders & Filters
        String shader = configHelper.getValue("Graphics", "Shader", "").trim();
        if (shader.contains("#")) shader = shader.substring(0, shader.indexOf('#')).trim();
        if (shader.contains(";")) shader = shader.substring(0, shader.indexOf(';')).trim();

        isUpdatingShaderUi = true;
        if (shader.isEmpty()) {
            cbEnableShader.setChecked(false);
            spShaderPreset.setSelection(0);
            tvShaderDesc.setText(SHADER_PRESETS.get(0).description);
            etShader.setText("");
            layoutCustomShader.setVisibility(View.GONE);
        } else {
            cbEnableShader.setChecked(true);
            int matchedIdx = -1;
            for (int i = 1; i < SHADER_PRESETS.size() - 1; i++) {
                ShaderPreset p = SHADER_PRESETS.get(i);
                if (p.path.equalsIgnoreCase(shader) ||
                        shader.endsWith(p.path) ||
                        p.path.endsWith(shader)) {
                    matchedIdx = i;
                    break;
                }
            }

            if (matchedIdx != -1) {
                spShaderPreset.setSelection(matchedIdx);
                tvShaderDesc.setText(SHADER_PRESETS.get(matchedIdx).description);
                etShader.setText(shader);
                layoutCustomShader.setVisibility(View.GONE);
            } else {
                int customIdx = SHADER_PRESETS.size() - 1;
                spShaderPreset.setSelection(customIdx);
                tvShaderDesc.setText(SHADER_PRESETS.get(customIdx).description);
                etShader.setText(shader);
                layoutCustomShader.setVisibility(View.VISIBLE);
            }
        }
        isUpdatingShaderUi = false;

        // Sound
        cbEnableAudio.setChecked(configHelper.getBoolValue("Sound", "EnableAudio", true));
        cbResumeMsu.setChecked(configHelper.getBoolValue("Sound", "ResumeMSU", true));

        String freq = String.valueOf(configHelper.getIntValue("Sound", "AudioFreq", 44100));
        setSpinnerSelection(spAudioFreq, AUDIO_FREQS, freq, "44100");

        String samples = String.valueOf(configHelper.getIntValue("Sound", "AudioSamples", 512));
        setSpinnerSelection(spAudioSamples, AUDIO_SAMPLES, samples, "512");

        String channels = String.valueOf(configHelper.getIntValue("Sound", "AudioChannels", 2));
        setSpinnerSelection(spAudioChannels, AUDIO_CHANNELS, channels, "2");

        String msu = configHelper.getValue("Sound", "EnableMSU", "false");
        setSpinnerSelection(spEnableMsu, MSU_OPTIONS, msu, "false");

        String msuVolStr = configHelper.getValue("Sound", "MSUVolume", "100%");
        int vol = 100;
        try {
            if (msuVolStr.endsWith("%")) msuVolStr = msuVolStr.substring(0, msuVolStr.length() - 1);
            vol = Integer.parseInt(msuVolStr.trim());
        } catch (Exception ignored) {}
        if (vol < 0) vol = 0;
        if (vol > 100) vol = 100;
        sbMsuVolume.setProgress(vol);
        tvMsuVolumeLabel.setText("Volume da Trilha MSU: " + vol + "%");

        // Features
        cbItemSwitchLr.setChecked(configHelper.getBoolValue("Features", "ItemSwitchLR", false));
        cbItemSwitchLimit.setChecked(configHelper.getBoolValue("Features", "ItemSwitchLRLimit", false));
        cbTurnWhileDashing.setChecked(configHelper.getBoolValue("Features", "TurnWhileDashing", false));
        cbMirrorDarkworld.setChecked(configHelper.getBoolValue("Features", "MirrorToDarkworld", false));
        cbCollectWithSword.setChecked(configHelper.getBoolValue("Features", "CollectItemsWithSword", false));
        cbBreakPots.setChecked(configHelper.getBoolValue("Features", "BreakPotsWithSword", false));
        cbDisableLowHealthBeep.setChecked(configHelper.getBoolValue("Features", "DisableLowHealthBeep", false));
        cbSkipIntro.setChecked(configHelper.getBoolValue("Features", "SkipIntroOnKeypress", false));
        cbShowMaxYellow.setChecked(configHelper.getBoolValue("Features", "ShowMaxItemsInYellow", false));
        cbMoreActiveBombs.setChecked(configHelper.getBoolValue("Features", "MoreActiveBombs", false));
        cbCarryMoreRupees.setChecked(configHelper.getBoolValue("Features", "CarryMoreRupees", false));
        cbMiscBugFixes.setChecked(configHelper.getBoolValue("Features", "MiscBugFixes", false));
        cbGameChangingBugFixes.setChecked(configHelper.getBoolValue("Features", "GameChangingBugFixes", false));
        cbCancelBirdTravel.setChecked(configHelper.getBoolValue("Features", "CancelBirdTravel", false));
        cbSkipDialogueA.setChecked(configHelper.getBoolValue("Features", "SkipDialogueOnHoldA", true));
        cbMaxHearts.setChecked(configHelper.getBoolValue("Features", "MaxHearts", false));

        // Raw
        etRawIni.setText(configHelper.getRawText());
    }

    private void syncUiToConfigHelper() {
        if (currentPanel == panelRawIni) {
            configHelper.setRawText(etRawIni.getText().toString());
            return;
        }

        // General
        configHelper.setValue("General", "ExtendedAspectRatio", spAspectRatio.getSelectedItem().toString());
        configHelper.setBoolValue("General", "Autosave", cbAutosave.isChecked());
        configHelper.setBoolValue("General", "DisableFrameDelay", cbDisableFrameDelay.isChecked());
        configHelper.setBoolValue("General", "DisplayPerfInTitle", cbDisplayPerf.isChecked());
        int selectedLangPos = spLanguage.getSelectedItemPosition();
        if (selectedLangPos >= 0 && selectedLangPos < LANGUAGE_OPTIONS.size()) {
            configHelper.setValue("General", "Language", LANGUAGE_OPTIONS.get(selectedLangPos).code);
        } else {
            configHelper.setValue("General", "Language", "pt");
        }

        // Graphics
        configHelper.setValue("Graphics", "OutputMethod", spOutputMethod.getSelectedItem().toString());
        configHelper.setValue("Graphics", "WindowScale", spWindowScale.getSelectedItem().toString());
        configHelper.setBoolValue("Graphics", "NewRenderer", cbNewRenderer.isChecked());
        configHelper.setBoolValue("Graphics", "EnhancedMode7", cbEnhancedMode7.isChecked());
        configHelper.setBoolValue("Graphics", "NoSpriteLimits", cbNoSpriteLimits.isChecked());
        configHelper.setBoolValue("Graphics", "LinearFiltering", cbLinearFiltering.isChecked());
        configHelper.setBoolValue("Graphics", "IgnoreAspectRatio", cbIgnoreAspectRatio.isChecked());
        configHelper.setBoolValue("Graphics", "DimFlashes", cbDimFlashes.isChecked());

        String linkGfx = etLinkGraphics.getText().toString().trim();
        if (!linkGfx.isEmpty()) {
            configHelper.setValue("Graphics", "LinkGraphics", linkGfx);
        } else {
            configHelper.removeKey("Graphics", "LinkGraphics");
        }

        // Shaders & Filters
        if (!cbEnableShader.isChecked() || spShaderPreset.getSelectedItemPosition() == 0) {
            configHelper.removeKey("Graphics", "Shader");
        } else {
            String shader = etShader.getText().toString().trim();
            if (!shader.isEmpty()) {
                configHelper.setValue("Graphics", "Shader", shader);
            } else {
                configHelper.removeKey("Graphics", "Shader");
            }
        }

        // Sound
        configHelper.setBoolValue("Sound", "EnableAudio", cbEnableAudio.isChecked());
        configHelper.setValue("Sound", "AudioFreq", spAudioFreq.getSelectedItem().toString());
        configHelper.setValue("Sound", "AudioSamples", spAudioSamples.getSelectedItem().toString());
        configHelper.setValue("Sound", "AudioChannels", spAudioChannels.getSelectedItem().toString());
        configHelper.setValue("Sound", "EnableMSU", spEnableMsu.getSelectedItem().toString());
        configHelper.setBoolValue("Sound", "ResumeMSU", cbResumeMsu.isChecked());
        configHelper.setValue("Sound", "MSUVolume", sbMsuVolume.getProgress() + "%");

        // Features
        configHelper.setBoolValue("Features", "ItemSwitchLR", cbItemSwitchLr.isChecked());
        configHelper.setBoolValue("Features", "ItemSwitchLRLimit", cbItemSwitchLimit.isChecked());
        configHelper.setBoolValue("Features", "TurnWhileDashing", cbTurnWhileDashing.isChecked());
        configHelper.setBoolValue("Features", "MirrorToDarkworld", cbMirrorDarkworld.isChecked());
        configHelper.setBoolValue("Features", "CollectItemsWithSword", cbCollectWithSword.isChecked());
        configHelper.setBoolValue("Features", "BreakPotsWithSword", cbBreakPots.isChecked());
        configHelper.setBoolValue("Features", "DisableLowHealthBeep", cbDisableLowHealthBeep.isChecked());
        configHelper.setBoolValue("Features", "SkipIntroOnKeypress", cbSkipIntro.isChecked());
        configHelper.setBoolValue("Features", "ShowMaxItemsInYellow", cbShowMaxYellow.isChecked());
        configHelper.setBoolValue("Features", "MoreActiveBombs", cbMoreActiveBombs.isChecked());
        configHelper.setBoolValue("Features", "CarryMoreRupees", cbCarryMoreRupees.isChecked());
        configHelper.setBoolValue("Features", "MiscBugFixes", cbMiscBugFixes.isChecked());
        configHelper.setBoolValue("Features", "GameChangingBugFixes", cbGameChangingBugFixes.isChecked());
        configHelper.setBoolValue("Features", "CancelBirdTravel", cbCancelBirdTravel.isChecked());
        configHelper.setBoolValue("Features", "SkipDialogueOnHoldA", cbSkipDialogueA.isChecked());
        configHelper.setBoolValue("Features", "MaxHearts", cbMaxHearts.isChecked());

        etRawIni.setText(configHelper.getRawText());
    }

    private void setupEvents() {
        tabGeneral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(tabGeneral, panelGeneral);
            }
        });

        tabGraphics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(tabGraphics, panelGraphics);
            }
        });

        tabSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(tabSound, panelSound);
            }
        });

        tabFeatures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(tabFeatures, panelFeatures);
            }
        });

        tabRawIni.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncUiToConfigHelper();
                selectTab(tabRawIni, panelRawIni);
            }
        });

        cbEnableShader.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isUpdatingShaderUi) return;
                isUpdatingShaderUi = true;
                if (isChecked) {
                    int pos = spShaderPreset.getSelectedItemPosition();
                    if (pos <= 0) {
                        pos = 1; // Default to Sharp Bilinear
                        spShaderPreset.setSelection(pos);
                    }
                    ShaderPreset preset = SHADER_PRESETS.get(pos);
                    tvShaderDesc.setText(preset.description);
                    if (pos == SHADER_PRESETS.size() - 1) {
                        layoutCustomShader.setVisibility(View.VISIBLE);
                    } else {
                        layoutCustomShader.setVisibility(View.GONE);
                        etShader.setText(preset.path);
                    }
                    // Auto-select OpenGL ES if currently on SDL
                    setSpinnerSelection(spOutputMethod, OUTPUT_METHODS, "OpenGL ES", "OpenGL ES");
                } else {
                    spShaderPreset.setSelection(0);
                    tvShaderDesc.setText(SHADER_PRESETS.get(0).description);
                    etShader.setText("");
                    layoutCustomShader.setVisibility(View.GONE);
                }
                isUpdatingShaderUi = false;
            }
        });

        spShaderPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingShaderUi) return;
                isUpdatingShaderUi = true;
                ShaderPreset preset = SHADER_PRESETS.get(position);
                tvShaderDesc.setText(preset.description);

                if (position == 0) { // Nenhum
                    cbEnableShader.setChecked(false);
                    etShader.setText("");
                    layoutCustomShader.setVisibility(View.GONE);
                } else if (position == SHADER_PRESETS.size() - 1) { // Personalizado
                    cbEnableShader.setChecked(true);
                    layoutCustomShader.setVisibility(View.VISIBLE);
                    setSpinnerSelection(spOutputMethod, OUTPUT_METHODS, "OpenGL ES", "OpenGL ES");
                } else { // Preset 1..9
                    cbEnableShader.setChecked(true);
                    layoutCustomShader.setVisibility(View.GONE);
                    etShader.setText(preset.path);
                    setSpinnerSelection(spOutputMethod, OUTPUT_METHODS, "OpenGL ES", "OpenGL ES");
                }
                isUpdatingShaderUi = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sbMsuVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMsuVolumeLabel.setText("Volume da Trilha MSU: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncUiToConfigHelper();
                if (configHelper.save()) {
                    MainActivity.reloadGameConfig();
                    Toast.makeText(ConfigActivity.this, "Configurações salvas e aplicadas em tempo real no jogo!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfigActivity.this, "Erro ao salvar zelda3.ini!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnReloadRaw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configHelper.load();
                loadValuesToUi();
                Toast.makeText(ConfigActivity.this, "Arquivo recarregado.", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnOpenItemEditor != null) {
            btnOpenItemEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showItemEditorDialog();
                }
            });
        }

        btnRestoreDefaults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ConfigActivity.this)
                        .setTitle("Restaurar Padrões")
                        .setMessage("Deseja restaurar todas as configurações do zelda3.ini para os valores padrão de fábrica?")
                        .setPositiveButton("Sim, Restaurar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (configHelper.restoreDefaults(ConfigActivity.this)) {
                                    loadValuesToUi();
                                    MainActivity.reloadGameConfig();
                                    Toast.makeText(ConfigActivity.this, "Configurações restauradas e aplicadas em tempo real!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ConfigActivity.this, "Erro ao restaurar configurações.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
    }

    private static final List<String> ITEM_SWORD_LIST = Arrays.asList(
            "0: Nenhuma Espada",
            "1: Fighter's Sword (Nível 1)",
            "2: Master Sword (Nível 2)",
            "3: Tempered Sword (Nível 3)",
            "4: Golden Sword (Nível 4)"
    );

    private static final List<String> ITEM_SHIELD_LIST = Arrays.asList(
            "0: Nenhum Escudo",
            "1: Fighter's Shield (Nível 1)",
            "2: Red Shield (Fogo - Nível 2)",
            "3: Mirror Shield (Espelhado - Nível 3)"
    );

    private static final List<String> ITEM_ARMOR_LIST = Arrays.asList(
            "0: Green Mail (Túnica Verde - Normal)",
            "1: Blue Mail (Túnica Azul - 1/2 Dano)",
            "2: Red Mail (Túnica Vermelha - 1/4 Dano)"
    );

    private static final List<String> ITEM_GLOVES_LIST = Arrays.asList(
            "0: Nenhuma Luva",
            "1: Power Glove (Nível 1 - Pedras Claras)",
            "2: Titan's Mitt (Nível 2 - Pedras Escuras)"
    );

    private static final List<String> ITEM_BOW_LIST = Arrays.asList(
            "0: Nenhum Arco",
            "1: Bow (Arco de Madeira sem flechas)",
            "2: Bow & Arrows (Arco e Flechas Comuns)",
            "3: Silver Bow (Arco de Prata sem flechas)",
            "4: Silver Bow & Arrows (Arco e Flechas de Prata)"
    );

    private static final List<String> ITEM_ARROWS_LIST = Arrays.asList(
            "0 Flechas", "10 Flechas", "20 Flechas", "30 Flechas", "40 Flechas", "50 Flechas", "60 Flechas", "70 Flechas"
    );

    private static final List<String> ITEM_BOOMERANG_LIST = Arrays.asList(
            "0: Nenhum Bumerangue",
            "1: Blue Boomerang (Azul)",
            "2: Magical Boomerang (Mágico Vermelho)"
    );

    private static final List<String> ITEM_MUSHROOM_LIST = Arrays.asList(
            "0: Nenhum",
            "1: Mushroom (Cogumelo)",
            "2: Magic Powder (Pó Mágico)"
    );

    private static final List<String> ITEM_FLUTE_LIST = Arrays.asList(
            "0: Nenhuma",
            "1: Shovel (Pá)",
            "2: Flute (Flauta Inativa)",
            "3: Active Flute (Flauta com Pássaro Ativado)"
    );

    private static final List<String> ITEM_BOMBS_LIST = Arrays.asList(
            "0 Bombas", "10 Bombas", "20 Bombas", "30 Bombas", "40 Bombas", "50 Bombas"
    );

    private static final List<String> ITEM_MAGIC_CONSUMPTION_LIST = Arrays.asList(
            "0: Normal (100% Consumo)",
            "1: 1/2 Consumo (Half Magic)",
            "2: 1/4 Consumo (Quarter Magic)"
    );

    private static final List<String> ITEM_BOTTLE_LIST = Arrays.asList(
            "0: Sem Garrafa",
            "1: Garrafa Vazia",
            "2: Poção Vermelha (Cura Vida)",
            "3: Poção Verde (Cura Magia)",
            "4: Poção Azul (Cura Vida + Magia)",
            "5: Fada (Reviver ao morrer)",
            "6: Abelha",
            "7: Abelha Dourada"
    );

    private static final List<String> ITEM_HEARTS_LIST = Arrays.asList(
            "3 Corações", "4 Corações", "5 Corações", "6 Corações", "7 Corações",
            "8 Corações", "9 Corações", "10 Corações", "11 Corações", "12 Corações",
            "13 Corações", "14 Corações", "15 Corações", "16 Corações", "17 Corações",
            "18 Corações", "19 Corações", "20 Corações (Vida Máxima)"
    );

    private void showItemEditorDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_inventory_editor);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Spinners
        final Spinner spSword = dialog.findViewById(R.id.sp_item_sword);
        final Spinner spShield = dialog.findViewById(R.id.sp_item_shield);
        final Spinner spArmor = dialog.findViewById(R.id.sp_item_armor);
        final Spinner spGloves = dialog.findViewById(R.id.sp_item_gloves);
        final Spinner spBow = dialog.findViewById(R.id.sp_item_bow);
        final Spinner spArrows = dialog.findViewById(R.id.sp_item_arrows);
        final Spinner spBoomerang = dialog.findViewById(R.id.sp_item_boomerang);
        final Spinner spMushroom = dialog.findViewById(R.id.sp_item_mushroom);
        final Spinner spFlute = dialog.findViewById(R.id.sp_item_flute);
        final Spinner spBombs = dialog.findViewById(R.id.sp_item_bombs);
        final Spinner spMagicConsumption = dialog.findViewById(R.id.sp_item_magic_consumption);
        final Spinner spBottle1 = dialog.findViewById(R.id.sp_item_bottle_1);
        final Spinner spBottle2 = dialog.findViewById(R.id.sp_item_bottle_2);
        final Spinner spBottle3 = dialog.findViewById(R.id.sp_item_bottle_3);
        final Spinner spBottle4 = dialog.findViewById(R.id.sp_item_bottle_4);
        final Spinner spHearts = dialog.findViewById(R.id.sp_item_hearts);

        // CheckBoxes - Tools
        final CheckBox cbHookshot = dialog.findViewById(R.id.cb_item_hookshot);
        final CheckBox cbTorch = dialog.findViewById(R.id.cb_item_torch);
        final CheckBox cbHammer = dialog.findViewById(R.id.cb_item_hammer);
        final CheckBox cbBugNet = dialog.findViewById(R.id.cb_item_bug_net);
        final CheckBox cbBookOfMudora = dialog.findViewById(R.id.cb_item_book_of_mudora);
        final CheckBox cbBoots = dialog.findViewById(R.id.cb_item_boots);
        final CheckBox cbFlippers = dialog.findViewById(R.id.cb_item_flippers);
        final CheckBox cbMoonPearl = dialog.findViewById(R.id.cb_item_moon_pearl);

        // CheckBoxes - Magic
        final CheckBox cbFireRod = dialog.findViewById(R.id.cb_item_fire_rod);
        final CheckBox cbIceRod = dialog.findViewById(R.id.cb_item_ice_rod);
        final CheckBox cbBombos = dialog.findViewById(R.id.cb_item_bombos);
        final CheckBox cbEther = dialog.findViewById(R.id.cb_item_ether);
        final CheckBox cbQuake = dialog.findViewById(R.id.cb_item_quake);
        final CheckBox cbCaneSomaria = dialog.findViewById(R.id.cb_item_cane_somaria);
        final CheckBox cbCaneByrna = dialog.findViewById(R.id.cb_item_cane_byrna);
        final CheckBox cbCape = dialog.findViewById(R.id.cb_item_cape);
        final CheckBox cbMirror = dialog.findViewById(R.id.cb_item_mirror);

        // CheckBoxes - Collectibles
        final CheckBox cbPendantCourage = dialog.findViewById(R.id.cb_pendant_courage);
        final CheckBox cbPendantWisdom = dialog.findViewById(R.id.cb_pendant_wisdom);
        final CheckBox cbPendantPower = dialog.findViewById(R.id.cb_pendant_power);

        final CheckBox cbCrystal1 = dialog.findViewById(R.id.cb_crystal_1);
        final CheckBox cbCrystal2 = dialog.findViewById(R.id.cb_crystal_2);
        final CheckBox cbCrystal3 = dialog.findViewById(R.id.cb_crystal_3);
        final CheckBox cbCrystal4 = dialog.findViewById(R.id.cb_crystal_4);
        final CheckBox cbCrystal5 = dialog.findViewById(R.id.cb_crystal_5);
        final CheckBox cbCrystal6 = dialog.findViewById(R.id.cb_crystal_6);
        final CheckBox cbCrystal7 = dialog.findViewById(R.id.cb_crystal_7);

        // Buttons
        Button btnPresetMax = dialog.findViewById(R.id.btn_preset_max);
        Button btnPresetClean = dialog.findViewById(R.id.btn_preset_clean);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_items);
        Button btnSaveApply = dialog.findViewById(R.id.btn_save_apply_items);

        // Adapters
        setupSpinnerAdapter(spSword, ITEM_SWORD_LIST);
        setupSpinnerAdapter(spShield, ITEM_SHIELD_LIST);
        setupSpinnerAdapter(spArmor, ITEM_ARMOR_LIST);
        setupSpinnerAdapter(spGloves, ITEM_GLOVES_LIST);
        setupSpinnerAdapter(spBow, ITEM_BOW_LIST);
        setupSpinnerAdapter(spArrows, ITEM_ARROWS_LIST);
        setupSpinnerAdapter(spBoomerang, ITEM_BOOMERANG_LIST);
        setupSpinnerAdapter(spMushroom, ITEM_MUSHROOM_LIST);
        setupSpinnerAdapter(spFlute, ITEM_FLUTE_LIST);
        setupSpinnerAdapter(spBombs, ITEM_BOMBS_LIST);
        setupSpinnerAdapter(spMagicConsumption, ITEM_MAGIC_CONSUMPTION_LIST);
        setupSpinnerAdapter(spBottle1, ITEM_BOTTLE_LIST);
        setupSpinnerAdapter(spBottle2, ITEM_BOTTLE_LIST);
        setupSpinnerAdapter(spBottle3, ITEM_BOTTLE_LIST);
        setupSpinnerAdapter(spBottle4, ITEM_BOTTLE_LIST);
        setupSpinnerAdapter(spHearts, ITEM_HEARTS_LIST);

        // Carregar estado atual da memória do jogo
        byte[] raw = MainActivity.getGameInventory();
        if (raw == null || raw.length < 64 || isAllZeros(raw)) {
            // Tentar ler do sram.dat se o jogo não estiver com a RAM instanciada
            raw = readInventoryFromSramFile();
        }

        if (raw != null && raw.length >= 64) {
            // Equipamentos
            int sword = raw[0x19] & 0xFF;
            spSword.setSelection(Math.min(sword, 4));

            int shield = raw[0x1A] & 0xFF;
            spShield.setSelection(Math.min(shield, 3));

            int armor = raw[0x1B] & 0xFF;
            spArmor.setSelection(Math.min(armor, 2));

            int gloves = raw[0x14] & 0xFF;
            spGloves.setSelection(Math.min(gloves, 2));

            // Ferramentas
            int bow = raw[0x00] & 0xFF;
            spBow.setSelection(Math.min(bow, 4));

            int arrows = raw[0x37] & 0xFF;
            spArrows.setSelection(Math.min(arrows / 10, 7));

            int boomerang = raw[0x01] & 0xFF;
            spBoomerang.setSelection(Math.min(boomerang, 2));

            int mushroom = raw[0x04] & 0xFF;
            spMushroom.setSelection(Math.min(mushroom, 2));

            int flute = raw[0x0C] & 0xFF;
            spFlute.setSelection(Math.min(flute, 3));

            int bombs = raw[0x03] & 0xFF;
            spBombs.setSelection(Math.min(bombs / 10, 5));

            cbHookshot.setChecked((raw[0x02] & 0xFF) != 0);
            cbTorch.setChecked((raw[0x0A] & 0xFF) != 0);
            cbHammer.setChecked((raw[0x0B] & 0xFF) != 0);
            cbBugNet.setChecked((raw[0x0D] & 0xFF) != 0);
            cbBookOfMudora.setChecked((raw[0x0E] & 0xFF) != 0);
            cbBoots.setChecked((raw[0x15] & 0xFF) != 0);
            cbFlippers.setChecked((raw[0x16] & 0xFF) != 0);
            cbMoonPearl.setChecked((raw[0x17] & 0xFF) != 0);

            // Itens Mágicos
            cbFireRod.setChecked((raw[0x05] & 0xFF) != 0);
            cbIceRod.setChecked((raw[0x06] & 0xFF) != 0);
            cbBombos.setChecked((raw[0x07] & 0xFF) != 0);
            cbEther.setChecked((raw[0x08] & 0xFF) != 0);
            cbQuake.setChecked((raw[0x09] & 0xFF) != 0);
            cbCaneSomaria.setChecked((raw[0x10] & 0xFF) != 0);
            cbCaneByrna.setChecked((raw[0x11] & 0xFF) != 0);
            cbCape.setChecked((raw[0x12] & 0xFF) != 0);
            cbMirror.setChecked((raw[0x13] & 0xFF) != 0);

            int magicCons = raw[0x3B] & 0xFF;
            spMagicConsumption.setSelection(Math.min(magicCons, 2));

            // Garrafas
            spBottle1.setSelection(Math.min(raw[0x1C] & 0xFF, 7));
            spBottle2.setSelection(Math.min(raw[0x1D] & 0xFF, 7));
            spBottle3.setSelection(Math.min(raw[0x1E] & 0xFF, 7));
            spBottle4.setSelection(Math.min(raw[0x1F] & 0xFF, 7));

            // Vida
            int maxHealth = (raw[0x2C] & 0xFF) / 8;
            if (maxHealth < 3) maxHealth = 3;
            if (maxHealth > 20) maxHealth = 20;
            spHearts.setSelection(maxHealth - 3);

            // Pingentes & Cristais
            int pendants = raw[0x34] & 0xFF;
            cbPendantPower.setChecked((pendants & 1) != 0);
            cbPendantWisdom.setChecked((pendants & 2) != 0);
            cbPendantCourage.setChecked((pendants & 4) != 0);

            int crystals = raw[0x3A] & 0xFF;
            cbCrystal1.setChecked((crystals & (1 << 0)) != 0);
            cbCrystal2.setChecked((crystals & (1 << 1)) != 0);
            cbCrystal3.setChecked((crystals & (1 << 2)) != 0);
            cbCrystal4.setChecked((crystals & (1 << 3)) != 0);
            cbCrystal5.setChecked((crystals & (1 << 4)) != 0);
            cbCrystal6.setChecked((crystals & (1 << 5)) != 0);
            cbCrystal7.setChecked((crystals & (1 << 6)) != 0);
        } else {
            // Valores padrão iniciais
            spSword.setSelection(0);
            spShield.setSelection(0);
            spArmor.setSelection(0);
            spGloves.setSelection(0);
            spBow.setSelection(0);
            spArrows.setSelection(0);
            spBoomerang.setSelection(0);
            spMushroom.setSelection(0);
            spFlute.setSelection(0);
            spBombs.setSelection(0);
            spMagicConsumption.setSelection(0);
            spBottle1.setSelection(0);
            spBottle2.setSelection(0);
            spBottle3.setSelection(0);
            spBottle4.setSelection(0);
            spHearts.setSelection(0); // 3 corações
        }

        // Preset: Tudo no Máximo
        final byte[] finalRaw = raw;
        btnPresetMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spSword.setSelection(4); // Golden Sword
                spShield.setSelection(3); // Mirror Shield
                spArmor.setSelection(2); // Red Mail
                spGloves.setSelection(2); // Titan's Mitt
                spBow.setSelection(4); // Silver Bow & Arrows
                spArrows.setSelection(7); // 70 Flechas
                spBoomerang.setSelection(2); // Magical Boomerang
                spMushroom.setSelection(2); // Magic Powder
                spFlute.setSelection(3); // Active Flute
                spBombs.setSelection(5); // 50 Bombas

                cbHookshot.setChecked(true);
                cbTorch.setChecked(true);
                cbHammer.setChecked(true);
                cbBugNet.setChecked(true);
                cbBookOfMudora.setChecked(true);
                cbBoots.setChecked(true);
                cbFlippers.setChecked(true);
                cbMoonPearl.setChecked(true);

                cbFireRod.setChecked(true);
                cbIceRod.setChecked(true);
                cbBombos.setChecked(true);
                cbEther.setChecked(true);
                cbQuake.setChecked(true);
                cbCaneSomaria.setChecked(true);
                cbCaneByrna.setChecked(true);
                cbCape.setChecked(true);
                cbMirror.setChecked(true);

                spMagicConsumption.setSelection(1); // 1/2 Magic

                spBottle1.setSelection(5); // Fada
                spBottle2.setSelection(5); // Fada
                spBottle3.setSelection(4); // Poção Azul
                spBottle4.setSelection(7); // Abelha Dourada

                spHearts.setSelection(17); // 20 Corações

                cbPendantCourage.setChecked(true);
                cbPendantWisdom.setChecked(true);
                cbPendantPower.setChecked(true);

                cbCrystal1.setChecked(true);
                cbCrystal2.setChecked(true);
                cbCrystal3.setChecked(true);
                cbCrystal4.setChecked(true);
                cbCrystal5.setChecked(true);
                cbCrystal6.setChecked(true);
                cbCrystal7.setChecked(true);

                Toast.makeText(ConfigActivity.this, "Preset 'Tudo no Máximo' selecionado!", Toast.LENGTH_SHORT).show();
            }
        });

        // Preset: Limpar Itens
        btnPresetClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spSword.setSelection(0);
                spShield.setSelection(0);
                spArmor.setSelection(0);
                spGloves.setSelection(0);
                spBow.setSelection(0);
                spArrows.setSelection(0);
                spBoomerang.setSelection(0);
                spMushroom.setSelection(0);
                spFlute.setSelection(0);
                spBombs.setSelection(0);

                cbHookshot.setChecked(false);
                cbTorch.setChecked(false);
                cbHammer.setChecked(false);
                cbBugNet.setChecked(false);
                cbBookOfMudora.setChecked(false);
                cbBoots.setChecked(false);
                cbFlippers.setChecked(false);
                cbMoonPearl.setChecked(false);

                cbFireRod.setChecked(false);
                cbIceRod.setChecked(false);
                cbBombos.setChecked(false);
                cbEther.setChecked(false);
                cbQuake.setChecked(false);
                cbCaneSomaria.setChecked(false);
                cbCaneByrna.setChecked(false);
                cbCape.setChecked(false);
                cbMirror.setChecked(false);

                spMagicConsumption.setSelection(0);

                spBottle1.setSelection(0);
                spBottle2.setSelection(0);
                spBottle3.setSelection(0);
                spBottle4.setSelection(0);

                spHearts.setSelection(0); // 3 Corações

                cbPendantCourage.setChecked(false);
                cbPendantWisdom.setChecked(false);
                cbPendantPower.setChecked(false);

                cbCrystal1.setChecked(false);
                cbCrystal2.setChecked(false);
                cbCrystal3.setChecked(false);
                cbCrystal4.setChecked(false);
                cbCrystal5.setChecked(false);
                cbCrystal6.setChecked(false);
                cbCrystal7.setChecked(false);

                Toast.makeText(ConfigActivity.this, "Inventário limpo.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSaveApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] data = (finalRaw != null && finalRaw.length >= 64) ? finalRaw.clone() : new byte[64];

                // Bow (0x00)
                data[0x00] = (byte) spBow.getSelectedItemPosition();
                // Boomerang (0x01)
                data[0x01] = (byte) spBoomerang.getSelectedItemPosition();
                // Hookshot (0x02)
                data[0x02] = (byte) (cbHookshot.isChecked() ? 1 : 0);
                // Bombs (0x03)
                int bombCount = spBombs.getSelectedItemPosition() * 10;
                data[0x03] = (byte) bombCount;
                data[0x30] = (byte) (bombCount > 10 ? 6 : 0);
                // Mushroom / Powder (0x04)
                data[0x04] = (byte) spMushroom.getSelectedItemPosition();
                // Fire Rod (0x05)
                data[0x05] = (byte) (cbFireRod.isChecked() ? 1 : 0);
                // Ice Rod (0x06)
                data[0x06] = (byte) (cbIceRod.isChecked() ? 1 : 0);
                // Bombos (0x07)
                data[0x07] = (byte) (cbBombos.isChecked() ? 1 : 0);
                // Ether (0x08)
                data[0x08] = (byte) (cbEther.isChecked() ? 1 : 0);
                // Quake (0x09)
                data[0x09] = (byte) (cbQuake.isChecked() ? 1 : 0);
                // Torch (0x0A)
                data[0x0A] = (byte) (cbTorch.isChecked() ? 1 : 0);
                // Hammer (0x0B)
                data[0x0B] = (byte) (cbHammer.isChecked() ? 1 : 0);
                // Flute / Shovel (0x0C)
                data[0x0C] = (byte) spFlute.getSelectedItemPosition();
                // Bug Net (0x0D)
                data[0x0D] = (byte) (cbBugNet.isChecked() ? 1 : 0);
                // Book of Mudora (0x0E)
                data[0x0E] = (byte) (cbBookOfMudora.isChecked() ? 1 : 0);
                // Cane of Somaria (0x10)
                data[0x10] = (byte) (cbCaneSomaria.isChecked() ? 1 : 0);
                // Cane of Byrna (0x11)
                data[0x11] = (byte) (cbCaneByrna.isChecked() ? 1 : 0);
                // Magic Cape (0x12)
                data[0x12] = (byte) (cbCape.isChecked() ? 1 : 0);
                // Magic Mirror (0x13)
                data[0x13] = (byte) (cbMirror.isChecked() ? 1 : 0);
                // Gloves (0x14)
                data[0x14] = (byte) spGloves.getSelectedItemPosition();
                // Pegasus Boots (0x15)
                data[0x15] = (byte) (cbBoots.isChecked() ? 1 : 0);
                // Zora's Flippers (0x16)
                data[0x16] = (byte) (cbFlippers.isChecked() ? 1 : 0);
                // Moon Pearl (0x17)
                data[0x17] = (byte) (cbMoonPearl.isChecked() ? 1 : 0);

                // Sword (0x19)
                data[0x19] = (byte) spSword.getSelectedItemPosition();
                // Shield (0x1A)
                data[0x1A] = (byte) spShield.getSelectedItemPosition();
                // Armor (0x1B)
                data[0x1B] = (byte) spArmor.getSelectedItemPosition();

                // Bottles 1..4 (0x1C..0x1F)
                data[0x1C] = (byte) spBottle1.getSelectedItemPosition();
                data[0x1D] = (byte) spBottle2.getSelectedItemPosition();
                data[0x1E] = (byte) spBottle3.getSelectedItemPosition();
                data[0x1F] = (byte) spBottle4.getSelectedItemPosition();

                // Bottle active index (0x0F)
                if (data[0x1C] != 0 || data[0x1D] != 0 || data[0x1E] != 0 || data[0x1F] != 0) {
                    if (data[0x0F] == 0) data[0x0F] = 1;
                }

                // Health capacity (0x2C)
                int hearts = spHearts.getSelectedItemPosition() + 3;
                data[0x2C] = (byte) (hearts * 8);
                data[0x2D] = (byte) (hearts * 8); // Vida cheia

                // Magic Power (0x2E)
                data[0x2E] = (byte) 0x80;

                // Pendants (0x34)
                int pendants = 0;
                if (cbPendantPower.isChecked()) pendants |= 1;
                if (cbPendantWisdom.isChecked()) pendants |= 2;
                if (cbPendantCourage.isChecked()) pendants |= 4;
                data[0x34] = (byte) pendants;

                // Arrows (0x37)
                int arrowCount = spArrows.getSelectedItemPosition() * 10;
                data[0x37] = (byte) arrowCount;
                data[0x31] = (byte) (arrowCount > 30 ? 7 : 0);

                // Ability flags (0x39)
                int abilities = 0;
                if (cbFlippers.isChecked()) abilities |= 1;
                if (cbBoots.isChecked()) abilities |= 2;
                data[0x39] = (byte) abilities;

                // Crystals (0x3A)
                int crystals = 0;
                if (cbCrystal1.isChecked()) crystals |= (1 << 0);
                if (cbCrystal2.isChecked()) crystals |= (1 << 1);
                if (cbCrystal3.isChecked()) crystals |= (1 << 2);
                if (cbCrystal4.isChecked()) crystals |= (1 << 3);
                if (cbCrystal5.isChecked()) crystals |= (1 << 4);
                if (cbCrystal6.isChecked()) crystals |= (1 << 5);
                if (cbCrystal7.isChecked()) crystals |= (1 << 6);
                data[0x3A] = (byte) crystals;

                // Magic consumption (0x3B)
                data[0x3B] = (byte) spMagicConsumption.getSelectedItemPosition();

                // Envia para a engine C via JNI e grava na SRAM
                MainActivity.setGameInventory(data);

                Toast.makeText(ConfigActivity.this, "Inventário atualizado e aplicado com sucesso no jogo!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean isAllZeros(byte[] arr) {
        if (arr == null) return true;
        for (byte b : arr) {
            if (b != 0) return false;
        }
        return true;
    }

    private byte[] readInventoryFromSramFile() {
        try {
            File externalDir = getExternalFilesDir(null);
            if (externalDir != null) {
                File sramFile = new File(externalDir, "saves/sram.dat");
                if (sramFile.exists() && sramFile.length() >= 0x380) {
                    byte[] buf = new byte[64];
                    try (FileInputStream fis = new FileInputStream(sramFile)) {
                        long skipped = fis.skip(0x340);
                        if (skipped == 0x340) {
                            int read = fis.read(buf);
                            if (read == 64) {
                                return buf;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void selectTab(Button tabButton, View panel) {
        if (currentTabButton != null) {
            currentTabButton.setTextColor(Color.parseColor("#B0C2DE"));
        }
        if (currentPanel != null) {
            currentPanel.setVisibility(View.GONE);
        }

        currentTabButton = tabButton;
        currentPanel = panel;

        currentTabButton.setTextColor(Color.parseColor("#FFD700"));
        currentPanel.setVisibility(View.VISIBLE);
    }
}
