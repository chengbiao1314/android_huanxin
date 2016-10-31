package com.cb.hxim_library.easeui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ListView;

import com.cb.hxim_library.easeui.adapter.EaseConversationAdapater;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMMessage;
import com.cb.hxim_library.R;

import java.util.ArrayList;
import java.util.List;

public class EaseConversationList extends ListView {
    
    protected int primaryColor;
    protected int secondaryColor;
    protected int timeColor;
    protected int primarySize;
    protected int secondarySize;
    protected float timeSize;
    

    protected final int MSG_REFRESH_ADAPTER_DATA = 0;
    
    protected Context context;
    protected EaseConversationAdapater adapter;
    protected List<EMConversation> conversationList = new ArrayList<EMConversation>();
    
    
    public EaseConversationList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public EaseConversationList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    
    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EaseConversationList);
        primaryColor = ta.getColor(R.styleable.EaseConversationList_cvsListPrimaryTextColor, R.color.list_itease_primary_color);
        secondaryColor = ta.getColor(R.styleable.EaseConversationList_cvsListSecondaryTextColor, R.color.list_itease_secondary_color);
        timeColor = ta.getColor(R.styleable.EaseConversationList_cvsListTimeTextColor, R.color.list_itease_secondary_color);
        primarySize = ta.getDimensionPixelSize(R.styleable.EaseConversationList_cvsListPrimaryTextSize, 0);
        secondarySize = ta.getDimensionPixelSize(R.styleable.EaseConversationList_cvsListSecondaryTextSize, 0);
        timeSize = ta.getDimension(R.styleable.EaseConversationList_cvsListTimeTextSize, 0);
        
        ta.recycle();
        
    }
    
    public void init(List<EMConversation> conversationList){
        this.init(conversationList, null);
    }
    
    public void init(List<EMConversation> conversationList, EaseConversationListHelper helper){
    	this.conversationList = conversationList;
    	if(helper != null){
    		this.conversationListHelper = helper;
    	}
        adapter = new EaseConversationAdapater(context, 0, conversationList);
        adapter.setCvsListHelper(conversationListHelper);
        adapter.setPrimaryColor(primaryColor);
        adapter.setPrimarySize(primarySize);
        adapter.setSecondaryColor(secondaryColor);
        adapter.setSecondarySize(secondarySize);
        adapter.setTimeColor(timeColor);
        adapter.setTimeSize(timeSize);
        setAdapter(adapter);
    }
    
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
            case MSG_REFRESH_ADAPTER_DATA:
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                break;
            default:
                break;
            }
        }
    };
    

    
    public EMConversation getItem(int position) {
        return (EMConversation)adapter.getItem(position);
    }
    
    public void refresh() {
        handler.sendEmptyMessage(MSG_REFRESH_ADAPTER_DATA);
    }
    
    public void filter(CharSequence str) {
        adapter.getFilter().filter(str);
    }
    
    
    /**
	 * 设置item中的头像形状
	 * 0：默认，1：圆形，2：矩形圆角
	 * @param shape
	 */
	public void setAvatarShape(int shape) {
		adapter.setAvatarShape(shape);
	}

	/**
	 * 设置头像控件边框宽度
	 * 
	 * @param width
	 */
	public void setAvatarBorderWidth(int width) {
		adapter.setBorderWidth(width);
	}

	/**
	 * 设置头像控件边框颜色
	 * 
	 * @param color
	 */
	public void setAvatarBorderColor(int color) {
		adapter.setBorderColor(color);
	}

	/**
	 * 设置头像控件圆角半径
	 * 
	 * @param radius
	 */
	public void setAvatarRadius(int radius) {
		adapter.setAvatarRadius(radius);
	}
	
	private EaseConversationListHelper conversationListHelper;
	public interface EaseConversationListHelper{
		/**
		 * 设置listview item次行内容
		 * @param lastMessage
		 * @return
		 */
		String onSetItemSecondaryText(EMMessage lastMessage);
	}
	public void setConversationListHelper(EaseConversationListHelper helper){
		conversationListHelper = helper;
	}
}
