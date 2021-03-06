package com.example.myeverytime.main.boards.in_post;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.myeverytime.BaseActivity;
import com.example.myeverytime.CMRespDto;
import com.example.myeverytime.R;
import com.example.myeverytime.main.boards.freeboard.FreeBoardActivity;
import com.example.myeverytime.main.boards.in_post.interfaces.InPostActivityView;
import com.example.myeverytime.main.boards.in_post.reply.ReplyAdapter;
import com.example.myeverytime.main.boards.in_post.reply.ReplyService;
import com.example.myeverytime.main.boards.in_post.reply.interfaces.ReplyActivityView;
import com.example.myeverytime.main.boards.in_post.reply.model.Reply;
import com.example.myeverytime.main.boards.in_post.reply.model.ReplySaveReqDto;
import com.example.myeverytime.main.boards.model.PostItem;
import com.example.myeverytime.main.boards.updating.UpdatingActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.myeverytime.SharedPreference.getAttribute;
import static com.example.myeverytime.SharedPreference.getAttributeLong;

public class InPostActivity extends BaseActivity implements InPostActivityView, ReplyActivityView, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "InPostActivity";
    private Context mContext;
    private Dialog deleteDialog;

    private ArrayList<Reply> m_reply_item_list;
    private RecyclerView rv_in_post_reply;
    private ReplyAdapter reply_adapter;
    private LinearLayoutManager linear_layout_manager;

    private CheckBox chk_in_reply_anonymous;
    private Boolean anonymous_checked = true;

    private TextView tv_in_post_nickname, tv_in_post_time, tv_in_post_title, tv_in_post_content, tv_in_post_like_num, tv_in_post_comment_num, tv_in_post_scrap_num;

    InputMethodManager imm;

    private InPostService inPostService;
    private String clicked;

    private EditText et_in_post_reply;
    private ImageView iv_in_post_register_reply;

    private int m_clicked_free_pos;
    private int m_clicked_secret_pos;
    private int m_clicked_alumni_pos;
    private int m_clicked_freshmen_pos;

    private int m_from_board_num;

    private int m_index_of_this_post;

    private boolean m_from_frag_home;

    private Long boardId; // ??????, ?????? ??????????????? ??????????????? intent??? ????????? boardId ( FreeBoardAdapter ?????? intent??? ????????? )

    public InPostActivity() {
    }

    public InPostActivity(ArrayList<Reply> m_reply_item_list, ReplyAdapter reply_adapter) {
        this.m_reply_item_list = m_reply_item_list;
        this.reply_adapter = reply_adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_post);

        mContext = this;

        // ????????? ???????????????
        deleteDialog = new Dialog(mContext);
        deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        deleteDialog.setContentView(R.layout.dialog_yes_no);

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); // ?????? ????????? ????????? ????????????
        m_reply_item_list = new ArrayList<>();

        reply_adapter = new ReplyAdapter(m_reply_item_list, mContext); // ?????? ?????????
        rv_in_post_reply = findViewById(R.id.rv_board_reply_list);

        linear_layout_manager = new LinearLayoutManager(getApplicationContext());
        rv_in_post_reply.setLayoutManager(linear_layout_manager);

        rv_in_post_reply.setAdapter(reply_adapter);

        // ??? ??????
        ViewBinding();

        // DB??? ????????? boardId ?????? ??????
        Intent intent = getIntent();
        boardId = intent.getLongExtra("freeBoardId", 0); // ( FreeBoardAdapter ?????? intent??? ????????? )

        // ?????? ?????? ????????????
        chk_in_reply_anonymous.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                anonymous_checked = true;
            }else {
                anonymous_checked = false;
            }
        });

        // ??????????????? ??????????????????, ??? ????????????, ???????????? ???????????? ??????
        tryGetOneFreeBoard(boardId);
        tryGetReply(boardId);

        // ?????? ?????? ??????, EditText??? ????????? ?????? ??????
        iv_in_post_register_reply.setOnClickListener(v -> {
            trySaveReply(boardId, et_in_post_reply.getText().toString());
        });

    }

    // ??? ??????
    public void ViewBinding() {

        chk_in_reply_anonymous = findViewById(R.id.chk_in_reply_anonymous);

        et_in_post_reply = findViewById(R.id.et_in_post_reply);
        iv_in_post_register_reply = findViewById(R.id.iv_in_post_register_reply);

        tv_in_post_nickname = findViewById(R.id.tv_in_post_nickname);
        tv_in_post_time = findViewById(R.id.tv_in_post_time);
        tv_in_post_title = findViewById(R.id.tv_in_post_title);
        tv_in_post_content = findViewById(R.id.tv_in_post_content);

        tv_in_post_like_num = findViewById(R.id.tv_in_post_like_num);
        tv_in_post_comment_num = findViewById(R.id.tv_in_post_comment_num);
        tv_in_post_scrap_num = findViewById(R.id.tv_in_post_scrap_num);
    }

    // ??? ?????? ?????? ??????
    private void tryGetOneFreeBoard(Long boardId){
        final InPostService inPostService = new InPostService(this);
        inPostService.getOneFreeBoard(boardId);
    }

    // ?????? ??????, ??????, ????????? ????????? ????????? ReplyController ?????? ?????????
    private void trySaveReply(Long boardId, String content){
        imm.hideSoftInputFromWindow(et_in_post_reply.getWindowToken(), 0);
        et_in_post_reply.setText("");
        ReplySaveReqDto replySaveReqDto = new ReplySaveReqDto(content);
        if(anonymous_checked){
            replySaveReqDto.setAnonymous(true);
            replySaveReqDto.setNickname("??????");
        }else{
            replySaveReqDto.setAnonymous(false);
            replySaveReqDto.setNickname(getAttribute(mContext, "loginUserNickname"));
        }

        final ReplyService replyService = new ReplyService(this);
        replyService.saveReply(boardId , getAttributeLong(mContext, "loginUserId"), replySaveReqDto);
    }

    // ??????????????? ??? ??????
    public void tryGetReply(Long boardId){
        m_reply_item_list.clear();
        final ReplyService replyService = new ReplyService(this);
        replyService.getReply(boardId);
    }


    // ????????????, ????????? ??????
    public void customOnClick2(View view) {
        switch (view.getId()) {
            case R.id.btn_in_post_go_back:
                onBackPressed();

                break;
            case R.id.btn_in_post_more:
                showPopUp(view);
                break;
        }

    }

    // ??? ??????, ?????? ?????? ??????
    public void showPopUp(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);

        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_board_delete_update);
        popupMenu.show();
    }

    // ????????? ???????????????
    public void showDeleteDialog(){
        deleteDialog.show();

        Button noBtn = deleteDialog.findViewById(R.id.noBtn);
        noBtn.setOnClickListener(v -> {
            deleteDialog.dismiss();
        });

        Button yesBtn = deleteDialog.findViewById(R.id.yesBtn);
        yesBtn.setOnClickListener(v -> {
            // ?????? ????????? ?????? ( ????????? ?????? ????????? ?????? BoardController ????????? ????????? )
            InPostService inPostService = new InPostService(this);
            inPostService.tryDeleteBoard(boardId, getAttributeLong(mContext, "loginUserId"));
            deleteDialog.dismiss();
        });
    }

    // ?????????


    // ???????????? ??????????????? ??????
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.post_delete:
                Log.d(TAG, "onMenuItemClick: ??? ?????? ?????? ??????");
                showDeleteDialog();
                return true;

            case R.id.post_update:
                Log.d(TAG, "onMenuItemClick: ??? ?????? ?????? ??????");
                InPostService inPostService = new InPostService(this);
                inPostService.principalCheck(boardId, getAttributeLong(mContext, "loginUserId"));
                return true;

            default:
                return false;
        }
    }

    // InPostActivityView ??????????????? ??????
    @Override
    public void validateSuccess(String text) {

    }

    // InPostActivityView ??????????????? ??????
    @Override
    public void validateFailure(String message) {
        Log.d(TAG, "validateFailure: ???????????????");
    }

    // InPostActivityView ??????????????? ??????
    @Override
    public void DeleteSuccess(CMRespDto cmRespDto) {
        switch (cmRespDto.getCode()) {
            case 100:
                Log.d(TAG, "DeleteSuccess: ??? ?????? ?????? code 100");
                AlertDialog.Builder dlg = new AlertDialog.Builder(InPostActivity.this);
                dlg.setTitle("???????????????");
                dlg.setMessage("?????? ?????? ???????????????.");
                dlg.setPositiveButton("??????", (dialog, which) -> {
                    Intent intent = new Intent(InPostActivity.this, FreeBoardActivity.class);
                    startActivity(intent);
                    finish();
                });
                dlg.show();

                break;
            default:
                AlertDialog.Builder dlg2 = new AlertDialog.Builder(InPostActivity.this);
                dlg2.setTitle("???????????????");
                dlg2.setMessage("???????????? ????????? ?????? ??? ????????????.");
                dlg2.setPositiveButton("??????", (dialog, which) -> {

                });
                dlg2.show();
                Log.d(TAG, "DeleteSuccess: code: " + cmRespDto.getCode());
                break;
        }
    }

    @Override
    public void principalCheckSuccess(CMRespDto cmRespDto) {
        switch (cmRespDto.getCode()) {
            case 100:
                Log.d(TAG, "principalCheck: ??? ?????? ?????? ?????? code 100");
                Intent intent = new Intent(InPostActivity.this, UpdatingActivity.class);
                intent.putExtra("boardName", 1);
                intent.putExtra("freeBoardId", boardId);
                intent.putExtra("title", tv_in_post_title.getText());
                intent.putExtra("content", tv_in_post_content.getText());
                startActivity(intent);


                break;
            default:
                AlertDialog.Builder dlg2 = new AlertDialog.Builder(InPostActivity.this);
                dlg2.setTitle("???????????????");
                dlg2.setMessage("???????????? ????????? ????????? ??? ????????????.");
                dlg2.setPositiveButton("??????", (dialog, which) -> {

                });
                dlg2.show();
                Log.d(TAG, "principalCheck: code: " + cmRespDto.getCode());
                break;
        }
    }

    // ReplyActivityView ??????????????? ??????
    @Override
    public void saveReplySuccess(CMRespDto cmRespDto) {
        switch (cmRespDto.getCode()) {
            case 100:
                Log.d(TAG, "saveReplySuccess: ?????? ?????? ?????? code 100");
                showCustomToast("?????? ?????? ??????");

                tryGetReply(boardId);

                break;
            default:
                showCustomToast("?????? ?????? ??????");
                break;
        }
    }

    // ReplyActivityView ??????????????? ??????
    @Override
    public void getReplySuccess(CMRespDto cmRespDto) {
        switch (cmRespDto.getCode()) {
            case 100:
                Log.d(TAG, "getReplySuccess: ?????? ?????? ?????? code 100");
                int num_of_reply_in_board = ((List<Reply>)cmRespDto.getData()).size();
                for(int i=0; i< num_of_reply_in_board; i++){
                    Reply getReplyItemData = ((List<Reply>)cmRespDto.getData()).get(i);
                    Reply reply = new Reply();

                    reply.setId(getReplyItemData.getId());
                    reply.setContent(getReplyItemData.getContent());
                    reply.setNickname(getReplyItemData.getNickname());
                    reply.setAnonymous(getReplyItemData.getAnonymous());
                    reply.setCreateDate(getReplyItemData.getCreateDate().substring(0,16));

                    Log.d(TAG, "getReplySuccess: ?????????? :" + cmRespDto.getData());
                    m_reply_item_list.add(reply);
                }
                reply_adapter.notifyDataSetChanged();
                break;
            default:
                Log.d(TAG, "getReplySuccess: ????????? 100??? ??????");
                break;
        }
    }

    // ReplyActivityView ??????????????? ?????? -> ReplyAdapter ?????? ?????????
    @Override
    public void deleteReplySuccess(CMRespDto cmRespDto) {

    }

    // InPostActivityView ??????????????? ??????
    @Override
    public void freeBoardSuccess(CMRespDto cmRespDto) {
        switch (cmRespDto.getCode()) {
            case 100:
                Log.d(TAG, "freeBoardSuccess: ??? ?????? ?????? ?????? code 100");
                PostItem postItem = (PostItem)cmRespDto.getData();

                tv_in_post_nickname.setText(postItem.getNickname());
                tv_in_post_time.setText(postItem.getCreateDate().substring(0,16));
                tv_in_post_title.setText(postItem.getTitle());
                tv_in_post_content.setText(postItem.getContent());

                // LikeNum , commentNum ?????? ?????? ?????????.
                break;
            default:
                Log.d(TAG, "freeBoardSuccess: ????????? 100??? ??????");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed:  inpost ?????? ???????????? ??????");
        Intent intent = new Intent(InPostActivity.this, FreeBoardActivity.class);
        startActivity(intent);
        finish();
    }
}