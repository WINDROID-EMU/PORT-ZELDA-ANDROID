package com.dishii.zelda3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    // General controls
    private Spinner spAspectRatio;
    private CheckBox cbAutosave, cbDisableFrameDelay, cbDisplayPerf;
    private EditText etLanguage;

    // Graphics controls
    private Spinner spOutputMethod, spWindowScale;
    private CheckBox cbNewRenderer, cbEnhancedMode7, cbNoSpriteLimits, cbLinearFiltering, cbIgnoreAspectRatio, cbDimFlashes;
    private EditText etLinkGraphics, etShader;

    // Sound controls
    private CheckBox cbEnableAudio, cbResumeMsu;
    private Spinner spAudioFreq, spAudioSamples, spAudioChannels, spEnableMsu;
    private SeekBar sbMsuVolume;
    private TextView tvMsuVolumeLabel;

    // Features controls
    private CheckBox cbItemSwitchLr, cbItemSwitchLimit, cbTurnWhileDashing, cbMirrorDarkworld,
            cbCollectWithSword, cbBreakPots, cbDisableLowHealthBeep, cbSkipIntro,
            cbShowMaxYellow, cbMoreActiveBombs, cbCarryMoreRupees, cbMiscBugFixes,
            cbGameChangingBugFixes, cbCancelBirdTravel, cbSkipDialogueA;

    // Raw INI
    private EditText etRawIni;

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
        etLanguage = findViewById(R.id.et_language);

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
    }

    private void setupSpinnerAdapter(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
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
        String lang = configHelper.getValue("General", "Language", "");
        if (lang != null && lang.contains("#")) lang = lang.substring(0, lang.indexOf('#')).trim();
        etLanguage.setText(lang);

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
        etShader.setText(configHelper.getValue("Graphics", "Shader", ""));

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
        String lang = etLanguage.getText().toString().trim();
        if (!lang.isEmpty()) {
            configHelper.setValue("General", "Language", lang);
        } else {
            configHelper.removeKey("General", "Language");
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

        String shader = etShader.getText().toString().trim();
        if (!shader.isEmpty()) {
            configHelper.setValue("Graphics", "Shader", shader);
        } else {
            configHelper.removeKey("Graphics", "Shader");
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
                    Toast.makeText(ConfigActivity.this, "Configurações salvas no zelda3.ini com sucesso!\n(Feche totalmente o jogo e abra de novo para aplicar)", Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(ConfigActivity.this, "Configurações restauradas para os padrões!", Toast.LENGTH_LONG).show();
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
