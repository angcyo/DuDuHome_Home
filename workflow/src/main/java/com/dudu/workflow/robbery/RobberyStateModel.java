package com.dudu.workflow.robbery;

/**
 * Created by Administrator on 2016/2/22.
 */
public class RobberyStateModel {
    public static final int START_ROBBERY = 0;
    public static final int ROBBERY_IS_TRIGGERED = 1;

    private int robberyState;

    public RobberyStateModel(int robberyState) {
        this.robberyState = robberyState;
    }

    public int getRobberyState() {
        return robberyState;
    }

    public void setRobberyState(int robberyState) {
        this.robberyState = robberyState;
    }
}
