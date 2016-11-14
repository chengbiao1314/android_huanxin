package com.cb.hxim_library.ui.fragment;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cb.hxim_library.Constant;
import com.cb.hxim_library.R;
import com.cb.hxim_library.db.InviteMessgeDao;
import com.cb.hxim_library.domain.HXUser;
import com.cb.hxim_library.domain.PageEnum;
import com.cb.hxim_library.easeui.ui.EaseConversationListFragment;
import com.cb.hxim_library.ui.ChatActivity;
import com.cb.hxim_library.ui.MainActivity;
import com.cb.hxim_library.ui.activity.ConversationListActivity;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMConversation.EMConversationType;
import com.easemob.util.NetUtils;

public class ConversationListFragment extends EaseConversationListFragment{

    private TextView errorText;

    @Override
    protected void initView() {
        super.initView();
        View errorView = (LinearLayout) View.inflate(getActivity(),R.layout.em_chat_neterror_item, null);
        errorItemContainer.addView(errorView);
        errorText = (TextView) errorView.findViewById(R.id.tv_connect_errormsg);
    }
    
    @Override
    protected void setUpView() {
        super.setUpView();

        // 设置标题栏点击事件
        titleBar.setLeftLayoutClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        // 注册上下文菜单
        registerForContextMenu(conversationListView);
        conversationListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EMConversation conversation = conversationListView.getItem(position);
                HXUser user = new HXUser();
                user.setTargetUserId(conversation.getUserName());
                if (user.getTargetUserId().equals(EMChatManager.getInstance().getCurrentUser()))
                    Toast.makeText(getActivity(), R.string.Cant_chat_with_yourself, Toast.LENGTH_SHORT).show();
                else {
                    // 进入聊天页面
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    if(conversation.isGroup()){
                        if(conversation.getType() == EMConversationType.ChatRoom){
                            // it's group chat
                            user.setTargetType(PageEnum.RoomPage);
                        }else{
                            user.setTargetType(PageEnum.GroupPage);
                        }

                    }else{
                        user.setTargetType(PageEnum.ChatPage);
                    }
                    // it's single chat
                    intent.putExtra(Constant.EXTRA_USER,user);
                    startActivity(intent);
                }


//                String username = conversation.getUserName();
//                if (username.equals(EMChatManager.getInstance().getCurrentUser()))
//                    Toast.makeText(getActivity(), R.string.Cant_chat_with_yourself, Toast.LENGTH_SHORT).show();
//                else {
//                    // 进入聊天页面
//                    Intent intent = new Intent(getActivity(), ChatActivity.class);
//                    if(conversation.isGroup()){
//                        if(conversation.getType() == EMConversationType.ChatRoom){
//                            // it's group chat
//                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_CHATROOM);
//                        }else{
//                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_GROUP);
//                        }
//
//                    }
//                    // it's single chat
//                    intent.putExtra(Constant.EXTRA_USER_ID, username);
//                    startActivity(intent);
//                }
            }
        });
    }

    @Override
    protected void onConnectionDisconnected() {
        super.onConnectionDisconnected();
        if (NetUtils.hasNetwork(getActivity())){
         errorText.setText(R.string.can_not_connect_chat_server_connection);
        } else {
          errorText.setText(R.string.the_current_network);
        }
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.em_delete_message, menu); 
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean handled = false;
        boolean deleteMessage = false;
        /*if (item.getItemId() == R.id.delete_message) {
            deleteMessage = true;
            handled = true;
        } else*/ if (item.getItemId() == R.id.delete_conversation) {
            deleteMessage = true;
        	EMConversation tobeDeleteCons = conversationListView.getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
            // 删除此会话
            EMChatManager.getInstance().deleteConversation(tobeDeleteCons.getUserName(), tobeDeleteCons.isGroup(), deleteMessage);
            InviteMessgeDao inviteMessgeDao = new InviteMessgeDao(getActivity());
            inviteMessgeDao.deleteMessage(tobeDeleteCons.getUserName());
            refresh();

            // 更新消息未读数
//            ((MainActivity) getActivity()).updateUnreadLabel();
        }
        return true;
    }

}
