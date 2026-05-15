package br.com.louvor4.api.enums;

public enum SkillIcon {
    ACOUSTIC_GUITAR("acoustic-guitar"),
    ELECTRIC_GUITAR("electric-guitar"),
    PIANO("piano"),
    KEYBOARD("keyboard"),
    DRUMS("drum"),
    MIC_VOCAL("mic-vocal"),
    BASS_GUITAR("bass-guitar"),
    MUSIC("music");

    private final String key;

    SkillIcon(String key) { this.key = key; }
    public String getKey() { return key; }
}
