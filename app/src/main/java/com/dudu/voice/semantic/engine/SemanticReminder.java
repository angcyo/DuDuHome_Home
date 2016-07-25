package com.dudu.voice.semantic.engine;

import com.dudu.android.launcher.R;
import com.dudu.commonlib.CommonLib;

import java.util.Random;

/**
 * Created by lxh on 2016-06-03 11:51.
 */
public class SemanticReminder {

    private String[] understaning;
    private String[] noSupport;
    private String[] no_input;
    private String[] net_workerror;
    private String[] choose_overflow;
    private String[] choose_reminder;
    private String[] strategy_reminder;

    public enum ReminderType {

        UNDERSTAND_MISUNDERSTAND,

        NO_SUPPORT,

        ERROR_NO_INPUT,

        NETWORK_ERROR,

        CHOOSE_OVER_FLOW,

        CHOOSE_REMINDER,

        STRATEGY_REMINDER
    }

    public SemanticReminder() {
        understaning = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.unsderstand_str);
        noSupport = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.no_support);
        no_input = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.understand_no_input);
        net_workerror = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.netword_error);
        choose_overflow = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.choose_overflow);
        choose_reminder = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.choose_reminder);
        strategy_reminder = CommonLib.getInstance().getContext().getResources().getStringArray(R.array.reminder_Strategy);
    }

    public String getReminder(ReminderType type) {
        Random rad = new Random();
        String playText = "";
        switch (type) {
            case UNDERSTAND_MISUNDERSTAND:
                playText = understaning[rad.nextInt(understaning.length)];
                break;
            case NETWORK_ERROR:
                playText = net_workerror[rad.nextInt(net_workerror.length)];
                break;
            case ERROR_NO_INPUT:
                playText = no_input[rad.nextInt(no_input.length)];
                break;
            case NO_SUPPORT:
                playText = noSupport[rad.nextInt(noSupport.length)];
                break;
            case CHOOSE_OVER_FLOW:
                playText = choose_overflow[rad.nextInt(choose_overflow.length)];
                break;
            case CHOOSE_REMINDER:
                playText = choose_reminder[rad.nextInt(choose_reminder.length)];
                break;
            case STRATEGY_REMINDER:
                playText = strategy_reminder[rad.nextInt(strategy_reminder.length)];
                break;
        }
        return playText;
    }
}
