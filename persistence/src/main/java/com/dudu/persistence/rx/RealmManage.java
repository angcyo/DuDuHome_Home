package com.dudu.persistence.rx;

import android.content.Context;

import com.dudu.commonlib.CommonLib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by dengjun on 2016/4/5.
 * Description :
 */
public class RealmManage {
    private static RealmManage instance = null;
    private static Logger logger = LoggerFactory.getLogger("realm.RealmManage");

    private static final int REALM_VERSION_1 = 1;
    private static final int REALM_VERSION_2 = 2;

    private RealmManage() {

    }

    public static RealmManage getInstance() {
        if (instance == null) {
            synchronized (RealmManage.class) {
                if (instance == null) {
                    instance = new RealmManage();
                }
            }
        }
        return instance;
    }

    public static RealmConfiguration.Builder getDefaultConfig(Context context) {
        logger.debug("getDefaultConfig");
        return new RealmConfiguration.Builder(context)
                .schemaVersion(REALM_VERSION_2)
                .migration(realmMigration);
    }

    private static RealmMigration realmMigration = new RealmMigration() {
        @Override
        public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
            logger.debug("migrate.oldVersion:" + oldVersion + "; newVersion:" + newVersion);
            RealmSchema schema = realm.getSchema();

            //创建新的表格
            //if (oldVersion == 0) {
            //    schema.create("Person")
            //            .addField("name", String.class)
            //            .addField("age", int.class);
            //    oldVersion++;
            //}

            //表格添加字段
            if (oldVersion < REALM_VERSION_1) {
                if (!schema.get("RealRobberyMessage").hasField("robberyTrigger")) {
                    schema.get("RealRobberyMessage")
                            .addField("robberyTrigger", boolean.class);
                }
            }

            //表格添加字段
            if (oldVersion < REALM_VERSION_2) {
                schema.create("TireInfoSetDataRealm")
                        .addField("obied", String.class)
                        .addField("tireHighestTemperatureValue",String.class)
                        .addField("frontAxleTirePressureRangeLowest",String.class)
                        .addField("frontAxleTirePressureRangeHighest",String.class)
                        .addField("rearAxleTirePressureRangeLowest",String.class)
                        .addField("rearAxleTirePressureRangeHighest",String.class);
            }

            oldVersion++;
        }
    };

    public static Realm getRealm() {
        return Realm.getInstance(getDefaultConfig(CommonLib.getInstance().getContext()).build());
    }

}
