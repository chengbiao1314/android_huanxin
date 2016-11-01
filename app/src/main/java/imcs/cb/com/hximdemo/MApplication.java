package imcs.cb.com.hximdemo;

import android.app.Application;
import android.content.Context;

import com.cb.hxim_library.HXOperation;

/**
 * Created by Ricky on 2016/11/1.
 */
public class MApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        HXOperation.getInstance().initInOnCreate(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        HXOperation.getInstance().initInAttachBaseContext(this);
    }

}
