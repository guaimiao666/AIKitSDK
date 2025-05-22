package com.example.aikitsdk;

public class AIKitSDKParam {
    private String appId;
    private String apikey;
    private String apiSecret;
    private String workDir;
    private String[] abilities;
    private String keyWord; //唤醒词
    private int language; //语言

    public String getAppId() {
        return appId;
    }

    public String getApikey() {
        return apikey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getWorkDir() {
        return workDir;
    }

    public String[] getAbilities() {
        return abilities;
    }

    public String getKeyWord() {
        return keyWord;
    }

    public int getLanguage() {
        return language;
    }

    private AIKitSDKParam(Builder builder) {
        this.appId = builder.appId;
        this.apikey = builder.apikey;
        this.apiSecret = builder.apiSecret;
        this.workDir = builder.workDir;
        this.abilities = builder.abilities;
        this.keyWord = builder.keyWord;
        this.language = builder.language;
    }

    public static class Builder {
        private String appId;
        private String apikey;
        private String apiSecret;
        private String workDir;
        private String[] abilities;
        private String keyWord;
        private int language;

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder apikey(String apikey) {
            this.apikey = apikey;
            return this;
        }

        public Builder apiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
            return this;
        }

        public Builder workDir(String workDir) {
            this.workDir = workDir;
            return this;
        }

        public Builder abilities(String[] abilities) {
            this.abilities = abilities;
            return this;
        }

        public Builder keyWord(String keyWord) {
            this.keyWord = keyWord;
            return this;
        }

        public Builder language(int language) {
            this.language = language;
            return this;
        }

        public AIKitSDKParam build() {
            return new AIKitSDKParam(this);
        }
    }
}
