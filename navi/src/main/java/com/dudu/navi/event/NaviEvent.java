package com.dudu.navi.event;

/**
 * Created by lxh on 2015/11/26.
 */
public class NaviEvent {

    public enum FloatButtonEvent {

        SHOW,
        HIDE
    }

    public static class NaviVoiceBroadcast{
        private String naviVoice;
        private boolean isShow;
        public NaviVoiceBroadcast(String naviVoice,boolean isShow){
            this.naviVoice = naviVoice;
            this.isShow = isShow;
        }

        public String getNaviVoice() {
            return naviVoice;
        }

        public boolean isShow() {
            return isShow;
        }
    }

    public enum  SearchResultType{
        SUCCESS,
        FAIL
    }

    public static class SearchResult{
        SearchResultType type;
        String info;

        public SearchResult(SearchResultType type, String info) {
            this.type = type;
            this.info = info;
        }

        public SearchResultType getType() {
            return type;
        }

        public String getInfo() {
            return info;
        }
    }

    public static class ToMainFragmentEvent{

    }

}
