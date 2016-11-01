package imcs.cb.com.hximdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.cb.hxim_library.HXOperation;
import com.cb.hxim_library.domain.HXUser;
import com.cb.hxim_library.domain.PageEnum;

public class MainActivity extends Activity {
    private EditText et_id;
    private EditText et_psw;
    private EditText et_type;
    private EditText et_targetID;

    private Button btn_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_id = (EditText) findViewById(R.id.et_id);
        et_psw = (EditText) findViewById(R.id.et_psw);
        et_type = (EditText) findViewById(R.id.et_type);
        et_targetID = (EditText) findViewById(R.id.et_targetID);

        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PageEnum indexPage;
                if("1".equals(et_type.getText().toString().trim())){
                    indexPage = PageEnum.ConversationListPage;
                }else if("2".equals(et_type.getText().toString().trim())){
                    indexPage = PageEnum.ChatPage;
                }else if("3".equals(et_type.getText().toString().trim())){
                    indexPage = PageEnum.CSPage;
                }else{
                    indexPage = PageEnum.Setting;
                }

                HXUser user = new HXUser();
                user.setUserId(et_id.getText().toString().trim());
                user.setPassword(et_psw.getText().toString().trim());
                user.setTargetUserId(et_targetID.getText().toString().trim());

                HXOperation.getInstance().startChat(MainActivity.this,user,indexPage);
            }
        });
    }
}
