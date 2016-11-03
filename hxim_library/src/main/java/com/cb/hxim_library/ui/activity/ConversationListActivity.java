/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cb.hxim_library.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cb.hxim_library.Constant;
import com.cb.hxim_library.HXHelper;
import com.cb.hxim_library.R;
import com.cb.hxim_library.easeui.EaseConstant;
import com.cb.hxim_library.easeui.utils.EaseCommonUtils;
import com.cb.hxim_library.ui.BaseActivity;
import com.cb.hxim_library.ui.GroupsActivity;
import com.cb.hxim_library.ui.LoginActivity;
import com.cb.hxim_library.ui.fragment.ConversationListFragment;
import com.easemob.EMCallBack;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.EMMessage.Type;
import com.easemob.util.EMLog;
import com.easemob.util.NetUtils;

public class ConversationListActivity extends BaseActivity implements EMEventListener {

	protected static final String TAG = "ConversationListActivity";

	// 账号在别处登录
	public boolean isConflict = false;
	// 账号被移除
	private boolean isCurrentAccountRemoved = false;
	

	/**
	 * 检查当前用户是否被删除
	 */
	public boolean getCurrentAccountRemoved() {
		return isCurrentAccountRemoved;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.getBoolean(Constant.ACCOUNT_REMOVED, false)) {
			// 防止被移除后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
			// 三个fragment里加的判断同理
		    HXHelper.getInstance().logout(false,null);
			finish();
			//TODO
//			startActivity(new Intent(this, LoginActivity.class));
			Toast.makeText(ConversationListActivity.this,"登录信息失效，请重新登录",Toast.LENGTH_SHORT).show();
			return;
		} else if (savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false)) {
			// 防止被T后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
			// 三个fragment里加的判断同理
			finish();
			//TODO
			Toast.makeText(ConversationListActivity.this,"登录信息失效，请重新登录",Toast.LENGTH_SHORT).show();
//			startActivity(new Intent(this, LoginActivity.class));
			return;
		}
		setContentView(R.layout.cb_activity_conversationlist);

		if (getIntent().getBooleanExtra(Constant.ACCOUNT_CONFLICT, false) && !isConflictDialogShow) {
			showConflictDialog();
		} else if (getIntent().getBooleanExtra(Constant.ACCOUNT_REMOVED, false) && !isAccountRemovedDialogShow) {
			showAccountRemovedDialog();
		}

		conversationListFragment = new ConversationListFragment();
		// 添加显示第一个fragment
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, conversationListFragment).show(conversationListFragment).commit();
		
		// 注册群组和联系人监听
        HXHelper.getInstance().registerGroupAndContactListener();
		registerBroadcastReceiver();
		
		
		//内部测试方法，请忽略
        registerInternalDebugReceiver();
	}

	/**
	 * 监听事件
     */
	@Override
	public void onEvent(EMNotifierEvent event) {
		switch (event.getEvent()) {
		case EventNewMessage: // 普通消息
			EMMessage message = (EMMessage) event.getData();
			// 提示新消息
			HXHelper.getInstance().getNotifier().onNewMsg(message);

			refreshUIWithMessage();
			break;
		case EventOfflineMessage: {
			refreshUIWithMessage();
			break;
		}

		case EventConversationListChanged: {
			refreshUIWithMessage();
		    break;
		}
		case EventNewCMDMessage:
			EMMessage cmdMessage = (EMMessage) event.getData();
			//获取消息body
            CmdMessageBody cmdMsgBody = (CmdMessageBody) cmdMessage.getBody();
            final String action = cmdMsgBody.action;//获取自定义action
            if(action.equals(EaseConstant.EASE_ATTR_REVOKE)){
                EaseCommonUtils.receiveRevokeMessage(this, cmdMessage);
            }
			refreshUIWithMessage();
			break;
		case EventReadAck:
            // TODO 这里当此消息未加载到内存中时，ackMessage会为null，消息的删除会失败
		    EMMessage ackMessage = (EMMessage) event.getData();
		    EMConversation conversation = EMChatManager.getInstance().getConversation(ackMessage.getTo());
		    // 判断接收到ack的这条消息是不是阅后即焚的消息，如果是，则说明对方看过消息了，对方会销毁，这边也删除(现在只有txt iamge file三种消息支持 )
            if(ackMessage.getBooleanAttribute(EaseConstant.EASE_ATTR_READFIRE, false) 
                    && (ackMessage.getType() == Type.TXT
                    || ackMessage.getType() == Type.VOICE
                    || ackMessage.getType() == Type.IMAGE)){
                // 判断当前会话是不是只有一条消息，如果只有一条消息，并且这条消息也是阅后即焚类型，当对方阅读后，这边要删除，会话会被过滤掉，因此要加载上一条消息
                if(conversation.getAllMessages().size() == 1 && conversation.getLastMessage().getMsgId().equals(ackMessage.getMsgId())){
                    if (ackMessage.getChatType() == ChatType.Chat) {
                        conversation.loadMoreMsgFromDB(ackMessage.getMsgId(), 1);
                    } else {
                        conversation.loadMoreGroupMsgFromDB(ackMessage.getMsgId(), 1);
                    }
                }
                conversation.removeMessage(ackMessage.getMsgId());
            }
			refreshUIWithMessage();
		    break;
		default:
			break;
		}
	}

	/**
	 * 监听刷新会话列表
	 */
	private void refreshUIWithMessage() {
		runOnUiThread(new Runnable() {
			public void run() {
					// 当前页面如果为聊天历史页面，刷新此页面
					if (conversationListFragment != null) {
						conversationListFragment.refresh();
					}
			}
		});
	}

	@Override
	public void back(View view) {
		super.back(view);
	}
	
	private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_CONTACT_CHANAGED);
        intentFilter.addAction(Constant.ACTION_GROUP_CHANAGED);
        broadcastReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
				// 当前页面如果为聊天历史页面，刷新此页面
				if (conversationListFragment != null) {
					conversationListFragment.refresh();
				}

				String action = intent.getAction();
                if(action.equals(Constant.ACTION_GROUP_CHANAGED)){
                    if (EaseCommonUtils.getTopActivity(ConversationListActivity.this).equals(GroupsActivity.class.getName())) {
                        GroupsActivity.instance.onResume();
                    }
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }
	
	private void unregisterBroadcastReceiver(){
	    broadcastManager.unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();		
		
		if (conflictBuilder != null) {
			conflictBuilder.create().dismiss();
			conflictBuilder = null;
		}
		unregisterBroadcastReceiver();

		try {
            unregisterReceiver(internalDebugReceiver);
        } catch (Exception e) {
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		// unregister this event listener when this activity enters the
		// background
		HXHelper sdkHelper = HXHelper.getInstance();
		sdkHelper.pushActivity(this);

		// register the event listener when enter the foreground
		EMChatManager.getInstance().registerEventListener(this,
				new EMNotifierEvent.Event[] {
						EMNotifierEvent.Event.EventNewMessage,
						EMNotifierEvent.Event.EventOfflineMessage,
						EMNotifierEvent.Event.EventConversationListChanged,
						EMNotifierEvent.Event.EventNewCMDMessage,
						EMNotifierEvent.Event.EventReadAck
						});
		
		// if push service available, connect will be disconnected after app in background
		// after activity restore to foreground, reconnect 
		if (!EMChatManager.getInstance().isConnected() && NetUtils.hasNetwork(this)) {
		    EMChatManager.getInstance().reconnect();
		}
	}

	@Override
	protected void onStop() {
		EMChatManager.getInstance().unregisterEventListener(this);
		HXHelper sdkHelper = HXHelper.getInstance();
		sdkHelper.popActivity(this);

		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isConflict", isConflict);
		outState.putBoolean(Constant.ACCOUNT_REMOVED, isCurrentAccountRemoved);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
//			moveTaskToBack(false);
//			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private android.app.AlertDialog.Builder conflictBuilder;
	private android.app.AlertDialog.Builder accountRemovedBuilder;
	private boolean isConflictDialogShow;
	private boolean isAccountRemovedDialogShow;
    private BroadcastReceiver internalDebugReceiver;
    private ConversationListFragment conversationListFragment;
    private BroadcastReceiver broadcastReceiver;
    private LocalBroadcastManager broadcastManager;

	/**
	 * 显示帐号在别处登录dialog
	 */
	private void showConflictDialog() {
		isConflictDialogShow = true;
		HXHelper.getInstance().logout(false,null);
		String st = getResources().getString(R.string.Logoff_notification);
		if (!ConversationListActivity.this.isFinishing()) {
			// clear up global variables
			try {
				if (conflictBuilder == null)
					conflictBuilder = new android.app.AlertDialog.Builder(ConversationListActivity.this);
				conflictBuilder.setTitle(st);
				conflictBuilder.setMessage(R.string.connect_conflict);
				conflictBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						//TODO
						finish();
						Toast.makeText(ConversationListActivity.this,"登录信息失效，请重新登录",Toast.LENGTH_SHORT).show();
//						dialog.dismiss();
//						conflictBuilder = null;
//						finish();
//						startActivity(new Intent(ConversationListActivity.this, LoginActivity.class));
					}
				});
				conflictBuilder.setCancelable(false);
				conflictBuilder.create().show();
				isConflict = true;
			} catch (Exception e) {
				EMLog.e(TAG, "---------color conflictBuilder error" + e.getMessage());
			}

		}

	}

	/**
	 * 帐号被移除的dialog
	 */
	private void showAccountRemovedDialog() {
		isAccountRemovedDialogShow = true;
		HXHelper.getInstance().logout(false,null);
		String st5 = getResources().getString(R.string.Remove_the_notification);
		if (!ConversationListActivity.this.isFinishing()) {
			// clear up global variables
			try {
				if (accountRemovedBuilder == null)
					accountRemovedBuilder = new android.app.AlertDialog.Builder(ConversationListActivity.this);
				accountRemovedBuilder.setTitle(st5);
				accountRemovedBuilder.setMessage(R.string.em_user_remove);
				accountRemovedBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
//						dialog.dismiss();
//						accountRemovedBuilder = null;
//						finish();
//						startActivity(new Intent(ConversationListActivity.this, LoginActivity.class));
					}
				});
				accountRemovedBuilder.setCancelable(false);
				accountRemovedBuilder.create().show();
				isCurrentAccountRemoved = true;
			} catch (Exception e) {
				EMLog.e(TAG, "---------color userRemovedBuilder error" + e.getMessage());
			}

		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getBooleanExtra(Constant.ACCOUNT_CONFLICT, false) && !isConflictDialogShow) {
			showConflictDialog();
		} else if (intent.getBooleanExtra(Constant.ACCOUNT_REMOVED, false) && !isAccountRemovedDialogShow) {
			showAccountRemovedDialog();
		}
	}
	
	/**
	 * 内部测试代码，开发者请忽略
	 */
	private void registerInternalDebugReceiver() {
	    internalDebugReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                HXHelper.getInstance().logout(false,new EMCallBack() {
                    
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // 重新显示登陆页面
                                finish();
								Toast.makeText(ConversationListActivity.this,"登录信息失效，请重新登录",Toast.LENGTH_SHORT).show();
//                                startActivity(new Intent(ConversationListActivity.this, LoginActivity.class));
                                
                            }
                        });
                    }
                    
                    @Override
                    public void onProgress(int progress, String status) {}
                    
                    @Override
                    public void onError(int code, String message) {}
                });
            }
        };
        IntentFilter filter = new IntentFilter(getPackageName() + ".em_internal_debug");
        registerReceiver(internalDebugReceiver, filter);
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		//getMenuInflater().inflate(R.menu.context_tab_contact, menu);
	}
}
