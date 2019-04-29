package com.comp3050.hearthealthmonitor.activity;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.entity.MyMessage;
import com.comp3050.hearthealthmonitor.utility.C_Database;

public class MessageActivity extends AppCompatActivity {

    private String sortOrder = C_Database.TIMESTAMP + " DESC";
    private int sortOrderIndex = 0;
    private RecyclerView recyclerView;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        try {
            database = new DBHelper(this).getReadableDatabase();
            recyclerView = findViewById(R.id.recycler);
            updateRecycler();
        } catch (SQLiteException ex) {
            Toast.makeText(this, R.string.toast_db_updating, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.sort_msg:
                String[] items = new String[] {
                        getString(R.string.sort_time_desc),
                        getString(R.string.sort_time_asc),
                        getString(R.string.sort_type_desc),
                        getString(R.string.sort_type_asc)
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle(R.string.text_sort_msg);
                builder.setSingleChoiceItems(items, sortOrderIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newOrder = sortOrder;
                        switch (which) {
                            case 0:
                                newOrder = C_Database.TIMESTAMP + " DESC";
                                break;
                            case 1:
                                newOrder = C_Database.TIMESTAMP + " ASC";
                                break;
                            case 2:
                                newOrder = C_Database.IMPORTANCE + " DESC";
                                break;
                            case 3:
                                newOrder = C_Database.IMPORTANCE + " ASC";
                                break;
                        }
                        sortOrderIndex = which;
                        if (!newOrder.equals(sortOrder)) {
                            sortOrder = newOrder;
                            updateRecycler();
                        }
                        dialog.dismiss();
                    }
                }).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void showMessageDetail(int rowId) {
        if (database != null) {
            String selection = C_Database.ID + " =?";
            String[] selectionArgs = new String[] { String.valueOf(rowId) };
            Cursor cursor = database.query(DBHelper.TABLE_NAME_MSG,
                    null, selection, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle(cursor.getString(cursor.getColumnIndex(C_Database.TITLE)));
                builder.setMessage(cursor.getString(cursor.getColumnIndex(C_Database.CONTENT)));
                builder.show();
            }
            cursor.close();
        }
    }

    private void updateRecycler() {
        if (database != null) {
            Cursor cursor = database.query(DBHelper.TABLE_NAME_MSG,
                    null, null, null, null, null, sortOrder);
            if (cursor.moveToFirst()) {
                findViewById(R.id.textView_no_message).setVisibility(View.GONE);
                MyAdapter adapter = new MyAdapter(cursor);
                adapter.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position, int rowId) {
                        showMessageDetail(rowId);
                    }

                    @Override
                    public void onItemLongClick(View view, int position, int rowId) {
                        showMessageDetail(rowId);
                    }
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private interface OnItemClickListener {
        void onItemClick(View view, int position, int rowId);
        void onItemLongClick(View view, int position, int rowId);
    }

    private class MyAdapter extends RecyclerView.Adapter {

        private OnItemClickListener onItemClickListener;
        private final Cursor cursor;

        MyAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            return new MyViewHolder(LayoutInflater.from(MessageActivity.this).inflate(R.layout.card_view_message, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            final MyViewHolder myViewHolder = (MyViewHolder) viewHolder;
            if (cursor.moveToPosition(position)) {
                MyMessage.MessageType type = MyMessage.importanceToType(cursor.getInt(cursor.getColumnIndex(C_Database.IMPORTANCE)));
                myViewHolder.type.setText(type == null ? getString(R.string.text_unknow_type) : type.toString());
                myViewHolder.time.setText(cursor.getString(cursor.getColumnIndex(C_Database.TIMESTAMP)));
                myViewHolder.title.setText(cursor.getString(cursor.getColumnIndex(C_Database.TITLE)));
                myViewHolder.summary.setText(cursor.getString(cursor.getColumnIndex(C_Database.SUMMARY)));

                final int id = cursor.getInt(cursor.getColumnIndex(C_Database.ID));
                if (onItemClickListener != null) {
                    myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int pos = myViewHolder.getLayoutPosition();
                            onItemClickListener.onItemClick(myViewHolder.itemView, pos, id);
                        }
                    });

                    myViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            int pos = myViewHolder.getLayoutPosition();
                            onItemClickListener.onItemLongClick(myViewHolder.itemView, pos, id);
                            return true;
                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        private class MyViewHolder extends RecyclerView.ViewHolder {

            TextView time;
            TextView type;
            TextView title;
            TextView summary;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.message_time);
                type = itemView.findViewById(R.id.message_type);
                title = itemView.findViewById(R.id.message_title);
                summary = itemView.findViewById(R.id.message_summary);
            }
        }
    }
}
