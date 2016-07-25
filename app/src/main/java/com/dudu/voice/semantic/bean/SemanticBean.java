package com.dudu.voice.semantic.bean;

/**
 * Created by 赵圣琪 on 2015/12/24.
 */
public class SemanticBean {

    protected String service;

    protected String text;

    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {

        this.domain = domain;
    }

    // 标识是否有返回
    protected boolean hasResult = true;

    public static SemanticBean getDefaultBean(String text,String domain) {
        SemanticBean bean = new DefaultBean();
        bean.setHasResult(false);
        bean.setText(text);
        bean.setDomain(domain);
        return bean;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean hasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }

}
