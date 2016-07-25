package com.dudu.aios.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dudu.android.launcher.R;

import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/7/15.
 */
public class ProtocalActivity extends FragmentActivity implements View.OnClickListener {
    private ImageButton closeBtn;
    private TextView protocalTxt;
    private Subscription subscription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protocal);
        closeBtn = (ImageButton) findViewById(R.id.btn_close);
        protocalTxt = (TextView) findViewById(R.id.txt_protocal);
        closeBtn.setOnClickListener(this);

        subscription = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(getProtocal());
                }
                subscriber.onCompleted();

            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {
                        protocalTxt.setText(s);
                    }
                });

    }

    @Override
    public void onClick(View v) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscription != null) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }
    }

    private String getProtocal() {
        String protocal = "";
        try {
            InputStream is = getAssets().open("protocal.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            protocal = new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return protocal;
    }
}
