package br.com.louvor4.api.enums;

public enum FileCategory {
    MINISTRY_PROFILE("images/ministry/profile"),
    MINISTRY_COVER("images/ministry/cover"),
    PROJECT_PROFILE("images/project/profile"),
    USER_PROFILE("images/user/profile"),
    DOCUMENTS("documents");

    private final String path;

    FileCategory(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }
}
