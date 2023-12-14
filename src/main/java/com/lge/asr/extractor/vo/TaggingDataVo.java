package com.lge.asr.extractor.vo;

public class TaggingDataVo {

    // public String logId = "";
    public String engineType = "";
    public String tag_version = "";
    public String additionalData = null;

    public short max_abs = 0;
    public int zero_runs = 0;
    public int num_samples = 0;

    public double snr = 0;
    public double speech_level = 0;
    public double noise_level = 0;
    public double pitch = 0;
    public double rms_voc_mean = 0;
    public double rms_voc_std = 0;
    public double rms_sil_mean = 0;
    public double rms_sil_std = 0;
    public double rms_all_mean = 0;
    public double rms_all_std = 0;
    public double rms_voc_mean_db = 0;
    public double rms_sil_mean_db = 0;
    public double voc_ratio = 0;

    public int voc_cnt = 0;
    public int sil_cnt = 0;

    public String Voice_intensity = "";
    public String Noise_intensity = "";

    public short frame_loss = 0;
    public short amp_clipping = 0;
    public short voc_clipping = 0;

    public double rms_voc_max_db = 0;
    public double rms_sil_max_db = 0;
    public double rms_all_max_db = 0;
    public double rms_start_db = 0;
    public double rms_end_db = 0;

    public short num_speaker = 0;

    public double confidence1 = 0;
    public double confidence2 = 0;

    public short impulse_voc = 0;
    public short impulse_sil = 0;
    public short distortion = 0;

    public double reverberant = 0;
    public double speaking_rate = 0;
    public double start_sil_len = 0;
    public double end_sil_len = 0;
    public double voc_len = 0;
    public double total_len = 0;

    public String Gender = "";
    public String Age = "";
    public String Acoustic_scene = "";
    public String Acoustic_event = "";
    public String etc = "";

    public String reserved_1 = "";
    public String reserved_2 = "";
    public String reserved_3 = "";
    public String reserved_4 = "";
    public String reserved_5 = "";

}
