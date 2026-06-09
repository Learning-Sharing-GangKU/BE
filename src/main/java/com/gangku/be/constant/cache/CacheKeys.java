package com.gangku.be.constant.cache;

public final class CacheKeys {

    private CacheKeys() {}

    public static final String PREFIX_HOME_LATEST = "home:latest:";
    public static final String PREFIX_HOME_POPULAR = "home:popular:";
    public static final String PREFIX_HOME_RECOMMEND = "home:recommend:";

    public static String homeLatest(int page, int size) {
        return PREFIX_HOME_LATEST + "p" + page + ":s" + size;
    }

    public static String homePopular(int page, int size) {
        return PREFIX_HOME_POPULAR + "p" + page + ":s" + size;
    }

    public static String homeRecommend(Long userId, int page, int size) {
        String userSegment = userId != null ? userId.toString() : "anonymous";
        return PREFIX_HOME_RECOMMEND + userSegment + ":p" + page + ":s" + size;
    }
}
