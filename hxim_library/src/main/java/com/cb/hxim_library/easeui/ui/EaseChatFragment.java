package com.cb.hxim_library.easeui.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cb.hxim_library.Constant;
import com.cb.hxim_library.HXHelper;
import com.cb.hxim_library.R;
import com.cb.hxim_library.chatrow.ChatRowEvaluation;
import com.cb.hxim_library.domain.HXUser;
import com.cb.hxim_library.domain.PageEnum;
import com.cb.hxim_library.easeui.EaseConstant;
import com.cb.hxim_library.easeui.controller.EaseUI;
import com.cb.hxim_library.easeui.domain.EaseEmojicon;
import com.cb.hxim_library.easeui.utils.EaseCommonUtils;
import com.cb.hxim_library.easeui.utils.EaseImageUtils;
import com.cb.hxim_library.easeui.utils.EaseUserUtils;
import com.cb.hxim_library.easeui.widget.EaseAlertDialog;
import com.cb.hxim_library.easeui.widget.EaseChatExtendMenu;
import com.cb.hxim_library.easeui.widget.EaseChatInputMenu;
import com.cb.hxim_library.easeui.widget.EaseChatMessageList;
import com.cb.hxim_library.easeui.widget.EaseVoiceRecorderView;
import com.cb.hxim_library.easeui.widget.chatrow.EaseChatRow;
import com.cb.hxim_library.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.easemob.EMChatRoomChangeListener;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.EMValueCallBack;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMChatRoom;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.EMMessage.Type;
import com.easemob.chat.ImageMessageBody;
import com.easemob.chat.TextMessageBody;
import com.easemob.util.EMLog;
import com.easemob.util.PathUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * 可以直接new出来使用的聊天对话页面fragment，
 * 使用时需调用setArguments方法传入chatType(会话类型)和userId(用户或群id)
 * app也可继承此fragment续写
 * <br/>
 * <br/>
 * 参数传入示例可查看demo里的ChatActivity
 *
 */
@SuppressLint("ResourceAsColor")
public class EaseChatFragment extends EaseBaseFragment implements EMEventListener {
    protected static final String TAG = "EaseChatFragment";
    protected static final int REQUEST_CODE_MAP = 1;
    protected static final int REQUEST_CODE_CAMERA = 2;
    protected static final int REQUEST_CODE_LOCAL = 3;

    // 是否处于阅后即焚状态的标志，true为阅后即焚状态：此状态下发送的消息都是阅后即焚的消息，暂时实现了文字和图片，false表示正常状态
    public boolean isReadFire = false;
    /**
     * 传入fragment的参数
     */
    protected Bundle fragmentArgs;
    protected int chatType;
    protected String toChatUsername;

    protected HXUser user;
    protected EaseChatMessageList messageList;
    protected EaseChatInputMenu inputMenu;

    protected EMConversation conversation;
    
    protected InputMethodManager inputManager;
    protected ClipboardManager clipboard;

    protected Handler handler = new Handler();
    protected File cameraFile;
    protected EaseVoiceRecorderView voiceRecorderView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected ListView listView;

    protected boolean isloading;
    protected boolean haveMoreData = true;
    protected int pagesize = 20;
    protected GroupListener groupListener;
    protected EMMessage contextMenuMessage;
    
    static final int ITEM_TAKE_PICTURE = 1;
    static final int ITEM_PICTURE = 2;
//    static final int ITEM_LOCATION = 3;
    
//    protected int[] itemStrings = { R.string.attach_take_pic, R.string.attach_picture, R.string.attach_location };
//    protected int[] itemdrawables = { R.drawable.ease_chat_takepic_selector, R.drawable.ease_chat_image_selector,
//            R.drawable.ease_chat_location_selector };
//    protected int[] itemIds = { ITEM_TAKE_PICTURE, ITEM_PICTURE, ITEM_LOCATION };
    protected int[] itemStrings = { R.string.attach_take_pic, R.string.attach_picture};
    protected int[] itemdrawables = { R.drawable.ease_chat_takepic_selector, R.drawable.ease_chat_image_selector,};
    protected int[] itemIds = { ITEM_TAKE_PICTURE, ITEM_PICTURE};

    private EMChatRoomChangeListener chatRoomChangeListener;
    private boolean isMessageListInited;
    protected MyItemClickListener extendMenuItemClickListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ease_fragment_chat, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        fragmentArgs = getArguments();
//        // 判断单聊还是群聊
//        chatType = fragmentArgs.getInt(EaseConstant.EXTRA_CHAT_TYPE, EaseConstant.CHATTYPE_SINGLE);
//        // 会话人或群组id
//        toChatUsername = fragmentArgs.getString(EaseConstant.EXTRA_USER_ID);
        user = (HXUser) fragmentArgs.getSerializable(Constant.EXTRA_USER);
        if(user != null){
            toChatUsername = user.getTargetUserId();
            if(user.getTargetType() == PageEnum.ChatPage || user.getTargetType() == PageEnum.CSPage){
                chatType = EaseConstant.CHATTYPE_SINGLE;
            }else if(user.getTargetType() == PageEnum.GroupPage){
                chatType = EaseConstant.CHATTYPE_GROUP;
            }else if(user.getTargetType() == PageEnum.RoomPage){
                chatType = EaseConstant.CHATTYPE_CHATROOM;
            }else{
                chatType = EaseConstant.CHATTYPE_SINGLE;
            }
        }

        Log.e("hx","user is targetType..." + user.getTargetType());
        Log.e("hx","user is targetUserId..." + user.getTargetUserId());
        Log.e("hx","user is CSGroupID..." + user.getCSGroupID());

        super.onActivityCreated(savedInstanceState);
    }

    /**
     * init view
     */
    protected void initView() {
        // 按住说话录音控件
        voiceRecorderView = (EaseVoiceRecorderView) getView().findViewById(R.id.voice_recorder);

        // 消息列表layout
        messageList = (EaseChatMessageList) getView().findViewById(R.id.message_list);
        if(chatType != EaseConstant.CHATTYPE_SINGLE)
            messageList.setShowUserNick(true);

        listView = messageList.getListView();

        extendMenuItemClickListener = new MyItemClickListener();
        inputMenu = (EaseChatInputMenu) getView().findViewById(R.id.input_menu);
        registerExtendMenuItem();
        // init input menu
        inputMenu.init(null);
        inputMenu.setChatInputMenuListener(new EaseChatInputMenu.ChatInputMenuListener() {

            @Override
            public void onSendMessage(String content) {
                // 发送文本消息
                sendTextMessage(content);
            }

            @Override
            public boolean onPressToSpeakBtnTouch(View v, MotionEvent event) {
                return voiceRecorderView.onPressToSpeakBtnTouch(v, event, new EaseVoiceRecorderView.EaseVoiceRecorderCallback() {
                    
                    @Override
                    public void onVoiceRecordComplete(String voiceFilePath, int voiceTimeLength) {
                        // 发送语音消息
                        sendVoiceMessage(voiceFilePath, voiceTimeLength);
                    }
                });
            }

            @Override
            public void onBigExpressionClicked(EaseEmojicon emojicon) {
                //发送大表情(动态表情)
                sendBigExpressionMessage(emojicon.getName(), emojicon.getIdentityCode());
            }
        });

        swipeRefreshLayout = messageList.getSwipeRefreshLayout();
        swipeRefreshLayout.setColorSchemeResources(R.color.holo_blue_bright, R.color.holo_green_light,
                R.color.holo_orange_light, R.color.holo_red_light);

        inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * 设置属性，监听等
     */
    protected void setUpView() {
       /* titleBar.setTitle(toChatUsername);
        if (chatType == EaseConstant.CHATTYPE_SINGLE) { // 单聊
            // 设置标题
            if(EaseUserUtils.getUserInfo(toChatUsername) != null){
                titleBar.setTitle(EaseUserUtils.getUserInfo(toChatUsername).getNick());
            }
            titleBar.setRightImageResource(R.drawable.ease_mm_title_remove);
        } else {
        	titleBar.setRightImageResource(R.drawable.ease_to_group_details_normal);
            if (chatType == EaseConstant.CHATTYPE_GROUP) {
                // 群聊
                EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
                if (group != null)
                    titleBar.setTitle(group.getGroupName());
                // 监听当前会话的群聊解散被T事件
                groupListener = new GroupListener();
                EMGroupManager.getInstance().addGroupChangeListener(groupListener);
            } else {
                onChatRoomViewCreation();
            }

        }
        if (chatType != EaseConstant.CHATTYPE_CHATROOM) {
            onConversationInit();
            onMessageListInit();
        }*/
        //TODO updata
        titleBar.setTitle(user.getTargetUserId());
        if (user.getTargetType() == PageEnum.ChatPage) { // 单聊
            // 设置标题
            if(EaseUserUtils.getUserInfo(user.getTargetUserId()) != null){
                titleBar.setTitle(EaseUserUtils.getUserInfo(user.getTargetUserId()).getNick());
            }
            titleBar.setRightImageResource(R.drawable.ease_mm_title_remove);
        } else if(user.getTargetType() == PageEnum.CSPage){//客服
            // 设置标题
            titleBar.setTitle("客服");
            titleBar.setRightImageResource(R.drawable.ease_mm_title_remove);
        }else {
        	titleBar.setRightImageResource(R.drawable.ease_to_group_details_normal);
            if (user.getTargetType() == PageEnum.GroupPage) {
                // 群聊
                EMGroup group = EMGroupManager.getInstance().getGroup(user.getTargetUserId());
                if (group != null)
                    titleBar.setTitle(group.getGroupName());
                // 监听当前会话的群聊解散被T事件
                groupListener = new GroupListener();
                EMGroupManager.getInstance().addGroupChangeListener(groupListener);
            } else {
                onChatRoomViewCreation();
            }

        }
        if (user.getTargetType() != PageEnum.RoomPage) {
            onConversationInit();
            onMessageListInit();
        }



        // 设置标题栏点击事件
        titleBar.setLeftLayoutClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        titleBar.setRightLayoutClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                    emptyHistory();
                } else {
                    toGroupDetails();
                }
            }
        });

        setRefreshLayoutListener();
        
        // show forward message if the message is not null
        String forward_msg_id = getArguments().getString("forward_msg_id");
        if (forward_msg_id != null) {
            // 发送要转发的消息
            forwardMessage(forward_msg_id);
        }


        //TODO if target user is kefu ,add message
        if(user.getTargetType() == PageEnum.CSPage) {
            setChatFragmentHelper(new EaseChatFragmentHelper() {
                @Override
                public void onSetMessageAttributes(EMMessage message) {
                    // 设置消息扩展属性

                    //设置用户信息（昵称，qq等）
                    setUserInfoAttribute(message);

                    //设置VisitorInfo 传递的信息将在iframe中显示 : PS :这个方法可能会导致客服端收不到消息
                    //setVisitorInfoSrc(message);

                    //指向某个技能组，技能组（客服分组）内将自动分配客服 pointToSkillGroup(message, groupName);
                    String groupID = user.getCSGroupID();
                    if (groupID != null && groupID.length() > 0) {
                        pointToSkillGroup(message, groupID);
                    }

                    //指向某个客服 , 当会话同时指定了客服和技能组时，以指定客服为准，指定技能组失效。
                    String agentID = user.getCSAgentID();
                    if (agentID != null && agentID.length() > 0) {
                        pointToAgentUser(message, agentID);
                    }
                }

                @Override
                public void onEnterToChatDetails() {

                }

                @Override
                public void onAvatarClick(String username) {

                }

                @Override
                public boolean onMessageBubbleClick(EMMessage message) {
                    return false;
                }

                @Override
                public void onMessageBubbleLongClick(EMMessage message) {

                }

                @Override
                public boolean onExtendMenuItemClick(int itemId, View view) {
                    return false;
                }

                @Override
                public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
                    // 设置自定义listview item提供者
                    Log.e("hx","onSetCustomChatRowProvider has running...");
                    return new CustomChatRowProvider();
                }
            });
        }
    }
    
    /**
     * by lzan13 
     * 设置阅后即焚模式的开关，在easeui中默认是关闭状态，需要在Demo层面调用此方法
     * @param isOpen 
     */
    public void setReadFire(boolean isOpen){
        if(chatType == EaseConstant.CHATTYPE_SINGLE && isOpen){
            isReadFire = true;
            titleBar.setBackgroundResource(R.color.top_bar_red_bg);
            titleBar.setFireImageResource(R.drawable.ease_fire_white_24dp);
            Toast.makeText(getActivity(), R.string.toast_read_fire_opened, Toast.LENGTH_LONG).show();
            // 设置阅后即焚开关监听
            titleBar.setFireClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // 根据当前状态，改变标题栏颜色，红色表示阅后即焚状态，蓝色表示正常状态
                    if(isReadFire){
                        titleBar.setBackgroundResource(R.color.top_bar_normal_bg);
                        Toast.makeText(getActivity(), R.string.toast_read_fire_close, Toast.LENGTH_LONG).show();
                        isReadFire = false;
                    }else{
                        titleBar.setBackgroundResource(R.color.top_bar_red_bg);
                        Toast.makeText(getActivity(), R.string.toast_read_fire_opened, Toast.LENGTH_LONG).show();
                        isReadFire = true;
                    }
                }
            });
        }else{
            titleBar.closeReadFire();
            titleBar.setBackgroundResource(R.color.top_bar_normal_bg);
        }
    }
    
    /**
     * 注册底部菜单扩展栏item; 覆盖此方法时如果不覆盖已有item，item的id需大于3
     */
    protected void registerExtendMenuItem(){
        for(int i = 0; i < itemStrings.length; i++){
            inputMenu.registerExtendMenuItem(itemStrings[i], itemdrawables[i], itemIds[i], extendMenuItemClickListener);
        }
    }
    
    
    protected void onConversationInit(){
        // 获取当前conversation对象
        conversation = EMChatManager.getInstance().getConversation(toChatUsername);
        // 把此会话的未读数置为0
        conversation.markAllMessagesAsRead();
        
        // 初始化db时，每个conversation加载数目是getChatOptions().getNumberOfMessagesLoaded
        // 这个数目如果比用户期望进入会话界面时显示的个数不一样，就多加载一些
        final List<EMMessage> msgs = conversation.getAllMessages();
        int msgCount = msgs != null ? msgs.size() : 0;
        if (msgCount < conversation.getAllMsgCount() && msgCount < pagesize) {
            String msgId = null;
            if (msgs != null && msgs.size() > 0) {
                msgId = msgs.get(0).getMsgId();
            }
            if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                conversation.loadMoreMsgFromDB(msgId, pagesize - msgCount);
            } else {
                conversation.loadMoreGroupMsgFromDB(msgId, pagesize - msgCount);
            }
        } 
        
    }
    
    protected void onMessageListInit(){
        messageList.init(toChatUsername, chatType, chatFragmentHelper != null ? 
                chatFragmentHelper.onSetCustomChatRowProvider() : null);
        //设置list item里的控件的点击事件
        setListItemClickListener();
        
        messageList.getListView().setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                inputMenu.hideExtendMenuContainer();
                return false;
            }
        });
        
        isMessageListInited = true;
    }
    
    protected void setListItemClickListener() {
        messageList.setItemClickListener(new EaseChatMessageList.MessageListItemClickListener() {
            
            @Override
            public void onUserAvatarClick(String username) {
                if(chatFragmentHelper != null){
                    chatFragmentHelper.onAvatarClick(username);
                }
            }
            
            @Override
            public void onResendClick(final EMMessage message) {
                new EaseAlertDialog(getActivity(), R.string.resend, R.string.confirm_resend, null, new EaseAlertDialog.AlertDialogUser() {
                    @Override
                    public void onResult(boolean confirmed, Bundle bundle) {
                        if (!confirmed) {
                            return;
                        }
                        resendMessage(message);
                    }
                }, true).show();
            }
            
            @Override
            public void onBubbleLongClick(EMMessage message) {
                contextMenuMessage = message;
                if(chatFragmentHelper != null){
                    chatFragmentHelper.onMessageBubbleLongClick(message);
                }
            }
            
            @Override
            public boolean onBubbleClick(EMMessage message) {
                if(chatFragmentHelper != null){
                    return chatFragmentHelper.onMessageBubbleClick(message);
                }
                return false; 
            }
        });
    }
    
    protected void setRefreshLayoutListener() {
        swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (listView.getFirstVisiblePosition() == 0 && !isloading && haveMoreData) {
                            List<EMMessage> messages;
                            try {
                                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                                    messages = conversation.loadMoreMsgFromDB(messageList.getItem(0).getMsgId(),
                                            pagesize);
                                } else {
                                    messages = conversation.loadMoreGroupMsgFromDB(messageList.getItem(0).getMsgId(),
                                            pagesize);
                                }
                            } catch (Exception e1) {
                                swipeRefreshLayout.setRefreshing(false);
                                return;
                            }
                            if (messages.size() > 0) {
                                messageList.refreshSeekTo(messages.size() - 1);
                                if (messages.size() != pagesize) {
                                    haveMoreData = false;
                                }
                            } else {
                                haveMoreData = false;
                            }

                            isloading = false;

                        } else {
                            Toast.makeText(getActivity(), getResources().getString(R.string.no_more_messages),
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 600);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA) { // 发送照片
                if (cameraFile != null && cameraFile.exists())
                    sendImageMessage(cameraFile.getAbsolutePath());
            } else if (requestCode == REQUEST_CODE_LOCAL) { // 发送本地图片
                if (data != null) {
                    Uri selectedImage = data.getData();
                    if (selectedImage != null) {
                        try{
                            sendPicByUri(selectedImage);
                        }catch(Exception e){
                            Toast.makeText(getActivity(),"无效图片", Toast.LENGTH_SHORT).show();
                            Log.e("发送图片异常：",e.toString());
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_MAP) { // 地图
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                String locationAddress = data.getStringExtra("address");
                if (locationAddress != null && !locationAddress.equals("")) {
                    sendLocationMessage(latitude, longitude, locationAddress);
                } else {
                    Toast.makeText(getActivity(), R.string.unable_to_get_loaction, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isMessageListInited){
            messageList.refresh();
        }
        EaseUI.getInstance().pushActivity(getActivity());
        // register the event listener when enter the foreground
        EMChatManager.getInstance().registerEventListener(
                this,
                new EMNotifierEvent.Event[] { EMNotifierEvent.Event.EventNewMessage,
                        EMNotifierEvent.Event.EventOfflineMessage,
                        EMNotifierEvent.Event.EventDeliveryAck,
                        EMNotifierEvent.Event.EventReadAck,
                        EMNotifierEvent.Event.EventNewCMDMessage});
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister this event listener when this activity enters the
        // background
        EMChatManager.getInstance().unregisterEventListener(this);

        // 把此activity 从foreground activity 列表里移除
        EaseUI.getInstance().popActivity(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (groupListener != null) {
            EMGroupManager.getInstance().removeGroupChangeListener(groupListener);
        }
        if(chatType == EaseConstant.CHATTYPE_CHATROOM){
            EMChatManager.getInstance().leaveChatRoom(toChatUsername);
        }
        
        if(chatRoomChangeListener != null){
            EMChatManager.getInstance().removeChatRoomChangeListener(chatRoomChangeListener);
        }
    }

    /**
     * 事件监听,registerEventListener后的回调事件
     * 
     * see {@link EMNotifierEvent}
     */
    @Override
    public void onEvent(EMNotifierEvent event) {
        switch (event.getEvent()) {
        case EventNewMessage:
            // 获取到message
            EMMessage message = (EMMessage) event.getData();

            String username = null;
            // 群组消息
            if (message.getChatType() == ChatType.GroupChat || message.getChatType() == ChatType.ChatRoom) {
                username = message.getTo();
            } else {
                // 单聊消息
                username = message.getFrom();
            }

            // 如果是当前会话的消息，刷新聊天页面
            if (username.equals(toChatUsername)) {
                messageList.refreshSelectLast();
                // 声音和震动提示有新消息
                EaseUI.getInstance().getNotifier().viberateAndPlayTone(message);
            } else {
                // 如果消息不是和当前聊天ID的消息
                EaseUI.getInstance().getNotifier().onNewMsg(message);
            }

            break;
        case EventDeliveryAck:
        case EventReadAck:
            // 获取到message
        	EMMessage ackMessage = (EMMessage) event.getData();
        	// 判断接收到ack的这条消息是不是阅后即焚的消息，如果是，则说明对方看过消息了，对方会销毁，这边也删除(现在只有txt iamge file三种消息支持 )
        	if(ackMessage.getBooleanAttribute(EaseConstant.EASE_ATTR_READFIRE, false) 
        	        && (ackMessage.getType() == Type.TXT || ackMessage.getType() == Type.VOICE || ackMessage.getType() == Type.IMAGE)){
        		conversation.removeMessage(ackMessage.getMsgId());
        	}
            messageList.refresh();
            break;
        case EventOfflineMessage:
            // a list of offline messages
            // List<EMMessage> offlineMessages = (List<EMMessage>)
            // event.getData();
            messageList.refresh();
            break;
        case EventNewCMDMessage:
        	EMMessage cmdMessage = (EMMessage) event.getData();
        	//获取消息body
            CmdMessageBody cmdMsgBody = (CmdMessageBody) cmdMessage.getBody();
            final String action = cmdMsgBody.action;//获取自定义action
            if(action.equals(EaseConstant.EASE_ATTR_REVOKE)){
            	EaseCommonUtils.receiveRevokeMessage(getActivity(), cmdMessage);
            	messageList.refresh();
            }
        	break;
        default:
            break;
        }

    }

    public void onBackPressed() {
        if (inputMenu.onBackPressed()) {
            getActivity().finish();
            if (chatType == EaseConstant.CHATTYPE_CHATROOM) {
                EMChatManager.getInstance().leaveChatRoom(toChatUsername);
            }
        }
    }

    protected void onChatRoomViewCreation() {
        final ProgressDialog pd = ProgressDialog.show(getActivity(), "", "Joining......");
        EMChatManager.getInstance().joinChatRoom(toChatUsername, new EMValueCallBack<EMChatRoom>() {

            @Override
            public void onSuccess(final EMChatRoom value) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(getActivity().isFinishing() || !toChatUsername.equals(value.getUsername()))
                            return;
                        pd.dismiss();
                        EMChatRoom room = EMChatManager.getInstance().getChatRoom(toChatUsername);
                        if (room != null) {
                            titleBar.setTitle(room.getName());
                        } else {
                            titleBar.setTitle(toChatUsername);
                        }
                        EMLog.d(TAG, "join room success : " + room.getName());
                        addChatRoomChangeListenr();
                        onConversationInit();
                        onMessageListInit();
                    }
                });
            }

            @Override
            public void onError(final int error, String errorMsg) {
                // TODO Auto-generated method stub
                EMLog.d(TAG, "join room failure : " + error);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                });
                getActivity().finish();
            }
        });
    }
    

    protected void addChatRoomChangeListenr() {
        chatRoomChangeListener = new EMChatRoomChangeListener() {

            @Override
            public void onChatRoomDestroyed(String roomId, String roomName) {
                if (roomId.equals(toChatUsername)) {
                    showChatroomToast(" room : " + roomId + " with room name : " + roomName + " was destroyed");
                    getActivity().finish();
                }
            }

            @Override
            public void onMemberJoined(String roomId, String participant) {
                showChatroomToast("member : " + participant + " join the room : " + roomId);
            }

            @Override
            public void onMemberExited(String roomId, String roomName, String participant) {
                showChatroomToast("member : " + participant + " leave the room : " + roomId + " room name : " + roomName);
            }

            @Override
            public void onMemberKicked(String roomId, String roomName, String participant) {
                if (roomId.equals(toChatUsername)) {
                    String curUser = EMChatManager.getInstance().getCurrentUser();
                    if (curUser.equals(participant)) {
                        EMChatManager.getInstance().leaveChatRoom(toChatUsername);
                        getActivity().finish();
                    }else{
                        showChatroomToast("member : " + participant + " was kicked from the room : " + roomId + " room name : " + roomName);
                    }
                }
            }

        };
        
        EMChatManager.getInstance().addChatRoomChangeListener(chatRoomChangeListener);
    }
    
    protected void showChatroomToast(final String toastContent){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), toastContent, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 扩展菜单栏item点击事件
     *
     */
    class MyItemClickListener implements EaseChatExtendMenu.EaseChatExtendMenuItemClickListener{

        @Override
        public void onClick(int itemId, View view) {
            if(chatFragmentHelper != null){
                if(chatFragmentHelper.onExtendMenuItemClick(itemId, view)){
                    return;
                }
            }
            switch (itemId) {
            case ITEM_TAKE_PICTURE: // 拍照
                selectPicFromCamera();
                break;
            case ITEM_PICTURE:
                selectPicFromLocal(); // 图库选择图片
                break;
//            case ITEM_LOCATION: // 位置
//                startActivityForResult(new Intent(getActivity(), EaseBaiduMapActivity.class), REQUEST_CODE_MAP);
//                break;

            default:
                break;
            }
        }

    }
    

    //发送消息方法
    //==========================================================================
    protected void sendTextMessage(String content) {
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        sendMessage(message);
    }
    
    protected void sendBigExpressionMessage(String name, String identityCode){
        EMMessage message = EaseCommonUtils.createExpressionMessage(toChatUsername, name, identityCode);
        sendMessage(message);
    }

    protected void sendVoiceMessage(String filePath, int length) {
        EMMessage message = EMMessage.createVoiceSendMessage(filePath, length, toChatUsername);
        sendMessage(message);
    }

    protected void sendImageMessage(String imagePath) {
        EMMessage message = EMMessage.createImageSendMessage(imagePath, false, toChatUsername);
        sendMessage(message);
    }

    protected void sendLocationMessage(double latitude, double longitude, String locationAddress) {
        EMMessage message = EMMessage.createLocationSendMessage(latitude, longitude, locationAddress, toChatUsername);
        sendMessage(message);
    }

    protected void sendVideoMessage(String videoPath, String thumbPath, int videoLength) {
        EMMessage message = EMMessage.createVideoSendMessage(videoPath, thumbPath, videoLength, toChatUsername);
        sendMessage(message);
    }

    protected void sendFileMessage(String filePath) {
        EMMessage message = EMMessage.createFileSendMessage(filePath, toChatUsername);
        sendMessage(message);
    }
    
    protected void sendMessage(EMMessage message){
        if(chatFragmentHelper != null){
            //设置扩展属性
            chatFragmentHelper.onSetMessageAttributes(message);
        }
        // 如果是群聊，设置chattype,默认是单聊
        if (chatType == EaseConstant.CHATTYPE_GROUP){
            message.setChatType(ChatType.GroupChat);
        }else if(chatType == EaseConstant.CHATTYPE_CHATROOM){
            message.setChatType(ChatType.ChatRoom);
        }
        //发送消息
        EMChatManager.getInstance().sendMessage(message, null);
        //刷新ui
        messageList.refreshSelectLast();
    }
    
    
    public void resendMessage(EMMessage message){
        message.status = EMMessage.Status.CREATE;
        EMChatManager.getInstance().sendMessage(message, null);
        messageList.refresh();
    }
    
    //===================================================================================
    

    /**
     * 根据图库图片uri发送图片
     * 
     * @param selectedImage
     */
    protected void sendPicByUri(Uri selectedImage) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            cursor = null;

            if (picturePath == null || picturePath.equals("null")) {
                Toast toast = Toast.makeText(getActivity(), R.string.cant_find_pictures, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            Log.e("picturePath 1------->",picturePath);
            sendImageMessage(picturePath);
        } else {
            File file = new File(selectedImage.getPath());
            if (!file.exists()) {
                Toast toast = Toast.makeText(getActivity(), R.string.cant_find_pictures, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;

            }
            Log.e("picturePath 2------->",file.getAbsolutePath());
            sendImageMessage(file.getAbsolutePath());
        }

    }
    
    /**
     * 根据uri发送文件
     * @param uri
     */
    protected void sendFileByUri(Uri uri){
        String filePath = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = null;

            try {
                cursor = getActivity().getContentResolver().query(uri, filePathColumn, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            Toast.makeText(getActivity(), R.string.File_does_not_exist, Toast.LENGTH_SHORT).show();
            return;
        }
        //大于10M不让发送
        if (file.length() > 10 * 1024 * 1024) {
            Toast.makeText(getActivity(), R.string.The_file_is_not_greater_than_10_m, Toast.LENGTH_SHORT).show();
            return;
        }
        sendFileMessage(filePath);
    }

    /**
     * 照相获取图片
     */
    protected void selectPicFromCamera() {
        if (!EaseCommonUtils.isExitsSdcard()) {
            Toast.makeText(getActivity(), R.string.sd_card_does_not_exist, Toast.LENGTH_SHORT).show();
            return;
        }

        cameraFile = new File(PathUtil.getInstance().getImagePath(), EMChatManager.getInstance().getCurrentUser()
                + System.currentTimeMillis() + ".jpg");
        cameraFile.getParentFile().mkdirs();
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraFile)),
                REQUEST_CODE_CAMERA);
    }

    /**
     * 从图库获取图片
     */
    protected void selectPicFromLocal() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_LOCAL);
    }



    /**
     * 点击清空聊天记录
     *
     */
    protected void emptyHistory() {
        String msg = getResources().getString(R.string.Whether_to_empty_all_chats);
        new EaseAlertDialog(getActivity(),null, msg, null,new EaseAlertDialog.AlertDialogUser() {

            @Override
            public void onResult(boolean confirmed, Bundle bundle) {
                if(confirmed){
                    // 清空会话
                    EMChatManager.getInstance().clearConversation(toChatUsername);
                    messageList.refresh();
                }
            }
        }, true).show();;
    }

    /**
     * 点击进入群组详情
     *
     */
    protected void toGroupDetails() {
        if (chatType == EaseConstant.CHATTYPE_GROUP) {
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            if (group == null) {
                Toast.makeText(getActivity(), R.string.gorup_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            if(chatFragmentHelper != null){
                chatFragmentHelper.onEnterToChatDetails();
            }
        }else if(chatType == EaseConstant.CHATTYPE_CHATROOM){
        	if(chatFragmentHelper != null){
        		chatFragmentHelper.onEnterToChatDetails();
        	}
        }
    }

    /**
     * 隐藏软键盘
     */
    protected void hideKeyboard() {
        if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getActivity().getCurrentFocus() != null)
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 转发消息
     *
     * @param forward_msg_id
     */
    protected void forwardMessage(String forward_msg_id) {
        final EMMessage forward_msg = EMChatManager.getInstance().getMessage(forward_msg_id);
        EMMessage.Type type = forward_msg.getType();
        switch (type) {
        case TXT:
            if(forward_msg.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)){
                sendBigExpressionMessage(((TextMessageBody) forward_msg.getBody()).getMessage(),
                        forward_msg.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXPRESSION_ID, null));
            }else{
                // 获取消息内容，发送消息
                String content = ((TextMessageBody) forward_msg.getBody()).getMessage();
                sendTextMessage(content);
            }
            break;
        case IMAGE:
            // 发送图片
            String filePath = ((ImageMessageBody) forward_msg.getBody()).getLocalUrl();
            if (filePath != null) {
                File file = new File(filePath);
                if (!file.exists()) {
                    // 不存在大图发送缩略图
                    filePath = EaseImageUtils.getThumbnailImagePath(filePath);
                }
                sendImageMessage(filePath);
            }
            break;
        default:
            break;
        }

        if(forward_msg.getChatType() == EMMessage.ChatType.ChatRoom){
            EMChatManager.getInstance().leaveChatRoom(forward_msg.getTo());
        }
    }

    /**
     * 监测群组解散或者被T事件
     * 
     */
    class GroupListener extends EaseGroupRemoveListener {

        @Override
        public void onUserRemoved(final String groupId, String groupName) {
            getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    if (toChatUsername.equals(groupId)) {
                        Toast.makeText(getActivity(), R.string.you_are_group, Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                }
            });
        }

        @Override
        public void onGroupDestroy(final String groupId, String groupName) {
            // 群组解散正好在此页面，提示群组被解散，并finish此页面
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (toChatUsername.equals(groupId)) {
                        Toast.makeText(getActivity(), R.string.the_current_group, Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                }
            });
        }

    }
    
   
    protected EaseChatFragmentHelper chatFragmentHelper;
    public void setChatFragmentHelper(EaseChatFragmentHelper chatFragmentHelper){
        this.chatFragmentHelper = chatFragmentHelper;
    }
    
    
    public interface EaseChatFragmentHelper{
        /**
         * 设置消息扩展属性
         */
        void onSetMessageAttributes(EMMessage message);
        
        /**
         * 进入会话详情
         */
        void onEnterToChatDetails();
        
        /**
         * 用户头像点击事件
         * @param username
         */
        void onAvatarClick(String username);
        
        /**
         * 消息气泡框点击事件
         */
        boolean onMessageBubbleClick(EMMessage message);
        
        /**
         * 消息气泡框长按事件
         */
        void onMessageBubbleLongClick(EMMessage message);
        
        /**
         * 扩展输入栏item点击事件,如果要覆盖EaseChatFragment已有的点击事件，return true
         * @param view 
         * @param itemId 
         * @return
         */
        boolean onExtendMenuItemClick(int itemId, View view);
        
        /**
         * 设置自定义chatrow提供者
         * @return
         */
        EaseCustomChatRowProvider onSetCustomChatRowProvider();
    }












    /**
     * 设置用户的属性，
     * 通过消息的扩展，传递客服系统用户的属性信息
     * @param message
     */
    private void setUserInfoAttribute(EMMessage message) {
        if (TextUtils.isEmpty(user.getNickname())) {
            user.setNickname(EMChatManager.getInstance().getCurrentUser());
        }
        JSONObject weichatJson = getWeichatJSONObject(message);
        try {
            JSONObject visitorJson = new JSONObject();

            visitorJson.put("userNickname",  user.getNickname());
            visitorJson.put("trueName", user.getName());
            visitorJson.put("qq", user.getQq());
            visitorJson.put("phone", user.getPhone());
            visitorJson.put("companyName", user.getCompanyName());
            visitorJson.put("description", user.getDescription());
            visitorJson.put("email", user.getEmail());

            weichatJson.put("visitor", visitorJson);

            message.setAttribute("weichat", weichatJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取消息中的扩展 weichat是否存在并返回jsonObject
     * @param message
     * @return
     */
    private JSONObject getWeichatJSONObject(EMMessage message){
        JSONObject weichatJson = null;
        try {
            String weichatString = message.getStringAttribute("weichat", null);
            if(weichatString == null){
                weichatJson = new JSONObject();
            }else{
                weichatJson = new JSONObject(weichatString);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return weichatJson;
    }

    /**
     * 指向某个具体客服，
     * @param message 消息
     * @param agentUsername 客服的登录账号
     */
    private void pointToAgentUser(EMMessage message, String agentUsername){
        try {
            JSONObject weichatJson = getWeichatJSONObject(message);
            weichatJson.put("agentUsername", agentUsername);
            message.setAttribute("weichat", weichatJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * 技能组（客服分组）发消息发到某个组
     * @param message 消息
     * @param groupName 分组名称
     */
    private void pointToSkillGroup(EMMessage message, String groupName){
        try {
            Log.v("********groupName:",groupName);
            JSONObject weichatJson = getWeichatJSONObject(message);
            weichatJson.put("queueName", groupName);
            message.setAttribute("weichat", weichatJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置自定义listview item提供者
     *
     */
    // evaluation
    private static final int MESSAGE_TYPE_SENT_EVAL = 1;
    private static final int MESSAGE_TYPE_RECV_EVAL = 2;
    public static final int REQUEST_CODE_EVAL = 26;
    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override
        public int getCustomChatRowTypeCount() {
            //此处返回的数目为getCustomChatRowType 中的布局的个数
            return 8;
        }

        @Override
        public int getCustomChatRowType(EMMessage message) {
            if (message.getType() == EMMessage.Type.TXT) {
                if (HXHelper.getInstance().isEvalMessage(message)) {
                    // 满意度评价
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_EVAL : MESSAGE_TYPE_SENT_EVAL;
                }
            }
            return 0;
        }

        @Override
        public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            Log.e("hx","getCustomChatRow has running...");
            if (message.getType() == EMMessage.Type.TXT) {
                if (HXHelper.getInstance().isEvalMessage(message)) {
                    return new ChatRowEvaluation(getActivity(), message, position, adapter);
                }
            }
            return null;
        }
    }






}
