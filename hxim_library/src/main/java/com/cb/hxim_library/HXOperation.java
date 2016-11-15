package com.cb.hxim_library;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.cb.hxim_library.domain.HXUser;
import com.cb.hxim_library.domain.PageEnum;
import com.cb.hxim_library.easeui.utils.EaseCommonUtils;
import com.cb.hxim_library.ui.activity.ChatActivity;
import com.cb.hxim_library.ui.activity.ConversationListActivity;
import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroupManager;

/**
 * Created by Ricky on 2016/11/1.
 */
public class HXOperation {
    public static Context applicationContext;
    private static HXOperation instance = null;

    private boolean progressShow;

    public static HXOperation getInstance(){
        if(instance == null){
            instance = new HXOperation();
        }
        return instance;
    }

    /**
     * 在 Applicaton 中的 onCreate方法中注册
     * @param context
     */
    public void initInOnCreate(Context context){
        MultiDex.install(context);
        applicationContext = context;

        //init demo helper
        HXHelper.getInstance().init(context);
    }

    /**
     * 在 Application 中的 initInAttachBaseContext方法中注册
     * @param context
     */
    public void initInAttachBaseContext(Context context){
        MultiDex.install(context);
    }


    /**
     * 全局都调用这个方法开启IM功能
     * @param context
     * @param user
     */
    public void startChat(final Context context , final HXUser user){
        //是上一次是同一个id，并且曾经成功登录过，直接跳过登录步骤
        if(HXHelper.getInstance().getCurrentUsernName() != null
                && HXHelper.getInstance().getCurrentUsernName().equals(user.getUserId())
                && HXHelper.getInstance().isLoggedIn()){
            skipPage(context,user);
            return;
        }

        /**
         * 执行登录流程
         */
        //网络连接有问题
        if (!EaseCommonUtils.isNetWorkConnected(context)) {
            Toast.makeText(context, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }

        //环信的用户id为空
        if (TextUtils.isEmpty(user.getUserId())) {
            Toast.makeText(context, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        //用户id对应的密码为空
        if (TextUtils.isEmpty(user.getPassword())) {
            Toast.makeText(context, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        progressShow = true;
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                progressShow = false;
            }
        });
        pd.setMessage(context.getString(R.string.Is_landing));
        pd.show();

        final long start = System.currentTimeMillis();
        // 调用sdk登陆方法登陆聊天服务器
        EMChatManager.getInstance().login(user.getUserId(), user.getPassword(), new EMCallBack() {

            @Override
            public void onSuccess() {
                if (!progressShow) {
                    return;
                }
                // 登陆成功，保存用户名
                HXHelper.getInstance().setCurrentUserName(user.getUserId());
                // 注册群组和联系人监听
                HXHelper.getInstance().registerGroupAndContactListener();

                // ** 第一次登录或者之前logout后再登录，加载所有本地群和回话
                // ** manually load all local groups and
                EMGroupManager.getInstance().loadAllGroups();
                EMChatManager.getInstance().loadAllConversations();

                //异步获取当前用户的昵称和头像(从自己服务器获取，demo使用的一个第三方服务)
                HXHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();

                if (pd.isShowing()) {
                    pd.dismiss();
                }

                skipPage(context,user);
            }

            @Override
            public void onProgress(int progress, String status) {
            }

            @Override
            public void onError(final int code, final String message) {
                if (!progressShow) {
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        pd.dismiss();
//                        Toast.makeText(context, context.getString(R.string.Login_failed) + message,Toast.LENGTH_SHORT).show();
                        Log.e("huanxin", "环信登录失败...");
                    }
                }).start();
            }
        });
    }

    /**
     * 跳转页面
     * @param context
     */
    private void skipPage(Context context ,HXUser user){
        switch (user.getTargetType()){
            case ConversationListPage:// 进入会话页面
                Intent intent0 = new Intent(context, ConversationListActivity.class);
                context.startActivity(intent0);
                break;
            case ChatPage:// 进入聊天页面
            case CSPage:// 进入客服页面
                Intent intent1 = new Intent(context, ChatActivity.class);
//                intent1.putExtra(Constant.EXTRA_USER_ID, user.getTargetUserId());
                intent1.putExtra(Constant.EXTRA_USER,user);
                context.startActivity(intent1);
                break;
            default:
                break;
        }
    }

}
