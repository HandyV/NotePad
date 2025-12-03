/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.SubMenu;


public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_CATEGORY,
            NotePad.Notes.COLUMN_NAME_COLOR
    };

    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_CATEGORY = 3;
    private static final int COLUMN_INDEX_COLOR = 4;

    // 添加成员变量
    private EditText mSearchEditText;
    private Button mSearchButton;
    private Button mClearButton;
    private View mSearchLayout;
    private String mCurrentSearchQuery = "";
    private String mCurrentSortOrder = NotePad.Notes.DEFAULT_SORT_ORDER;
    private int mCurrentColorFilter = -1; // -1表示不过滤
    private String mCurrentCategoryFilter = "";

    // 颜色图标资源数组
    private static final int[] COLOR_RESOURCES = {
            android.R.color.white,
            android.R.color.holo_red_light,
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_purple
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置应用主题
        setTheme(android.R.style.Theme_Holo_Light);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // 设置列表背景颜色
        getListView().setBackgroundColor(getResources().getColor(R.color.window_background));

        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                mCurrentSortOrder
        );

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_COLOR
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.note_time,
                R.id.note_category,
                R.id.note_color
        };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // 1. 处理时间显示
                if (view.getId() == R.id.note_time) {
                    TextView textView = (TextView) view;
                    long timeMillis = cursor.getLong(columnIndex);
                    String timeStr = formatDate(timeMillis);
                    textView.setText(timeStr);
                    textView.setTextColor(getResources().getColor(R.color.secondary_text));
                    return true;
                }
                // 2. 处理分类显示
                else if (view.getId() == R.id.note_category) {
                    TextView textView = (TextView) view;
                    String category = cursor.getString(columnIndex);
                    if (category != null && !category.isEmpty()) {
                        textView.setText(category);
                        textView.setVisibility(View.VISIBLE);
                        // 设置分类标签样式
                        textView.setBackgroundResource(R.drawable.category_background);
                        textView.setTextColor(getResources().getColor(R.color.primary_text));
                    } else {
                        textView.setVisibility(View.GONE);
                    }
                    return true;
                }
                // 3. 处理颜色圆点显示 (这是你已修改的部分)
                else if (view.getId() == R.id.note_color) {
                    ImageView imageView = (ImageView) view;
                    int colorValue = cursor.getInt(columnIndex); // 从数据库读取颜色值

                    // 根据笔记的color值设置对应的背景色
                    int backgroundColor;
                    switch (colorValue) {
                        case NotePad.Notes.COLOR_RED:
                            backgroundColor = getResources().getColor(R.color.color_red);
                            break;
                        case NotePad.Notes.COLOR_BLUE:
                            backgroundColor = getResources().getColor(R.color.color_blue);
                            break;
                        case NotePad.Notes.COLOR_GREEN:
                            backgroundColor = getResources().getColor(R.color.color_green);
                            break;
                        case NotePad.Notes.COLOR_YELLOW:
                            backgroundColor = getResources().getColor(R.color.color_yellow);
                            break;
                        case NotePad.Notes.COLOR_PURPLE:
                            backgroundColor = getResources().getColor(R.color.color_purple);
                            break;
                        case NotePad.Notes.COLOR_DEFAULT:
                        default:
                            // 默认颜色，设置为白色
                            backgroundColor = getResources().getColor(R.color.color_default);
                            break;
                    }

                    // 设置ImageView的背景色
                    imageView.setBackgroundColor(backgroundColor);
                    imageView.setVisibility(View.VISIBLE);

                    return true;
                }
                return false; // 其他视图未处理
            }
        });
        setListAdapter(adapter);
    }

    // 时间格式化方法
    private String formatDate(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    // 搜索相关方法
    private void performSearch() {
        mCurrentSearchQuery = mSearchEditText.getText().toString().trim();
        refreshList();
    }

    private void clearSearch() {
        if (mSearchEditText != null) {
            mSearchEditText.setText("");
        }
        mCurrentSearchQuery = "";
        mCurrentCategoryFilter = "";
        mCurrentColorFilter = -1;
        mCurrentSortOrder = NotePad.Notes.DEFAULT_SORT_ORDER;
        refreshList();
    }

    private void refreshList() {
        String selection = null;
        String[] selectionArgs = null;
        List<String> selectionList = new ArrayList<>();
        List<String> argsList = new ArrayList<>();

        if (mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty()) {
            selectionList.add("(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR "
                    + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)");
            argsList.add("%" + mCurrentSearchQuery + "%");
            argsList.add("%" + mCurrentSearchQuery + "%");
        }

        if (mCurrentCategoryFilter != null && !mCurrentCategoryFilter.isEmpty()) {
            selectionList.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
            argsList.add(mCurrentCategoryFilter);
        }

        if (mCurrentColorFilter >= 0) {
            selectionList.add(NotePad.Notes.COLUMN_NAME_COLOR + " = ?");
            argsList.add(String.valueOf(mCurrentColorFilter));
        }

        if (!selectionList.isEmpty()) {
            selection = TextUtils.join(" AND ", selectionList);
            selectionArgs = argsList.toArray(new String[0]);
        }

        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                mCurrentSortOrder
        );

        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.changeCursor(cursor);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 添加新的菜单项
        MenuItem sortItem = menu.add("排序");
        sortItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        sortItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        SubMenu sortSubMenu = menu.addSubMenu("排序方式");
        sortSubMenu.add(0, 1001, 0, "按时间降序");
        sortSubMenu.add(0, 1002, 1, "按时间升序");
        sortSubMenu.add(0, 1003, 2, "按标题");
        sortSubMenu.add(0, 1007, 3, "按颜色");

        SubMenu filterSubMenu = menu.addSubMenu("筛选");
        filterSubMenu.add(0, 1004, 0, "按分类筛选");
        filterSubMenu.add(0, 1005, 1, "按颜色筛选");
        filterSubMenu.add(0, 1006, 2, "清除筛选");

        // 添加搜索菜单项
        menu.add(0, 1008, 4, "搜索笔记")
                .setIcon(android.R.drawable.ic_menu_search)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            // 改为显式Intent
            Intent editIntent = new Intent(this, NoteEditor.class);
            editIntent.setAction(Intent.ACTION_EDIT);
            editIntent.setData(uri);
            specifics[0] = editIntent;

            MenuItem[] items = new MenuItem[1];

            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );

            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_add) {
            // 改为显式Intent
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_INSERT);
            intent.setData(getIntent().getData());
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_paste) {
            // 改为显式Intent
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_PASTE);
            intent.setData(getIntent().getData());
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_search) {
            showSearchDialog();
            return true;
        }

        switch (id) {
            case 1001: // 按时间降序
                mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";
                refreshList();
                return true;
            case 1002: // 按时间升序
                mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC";
                refreshList();
                return true;
            case 1003: // 按标题
                mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_TITLE + " ASC";
                refreshList();
                return true;
            case 1004: // 按分类筛选
                showCategoryFilterDialog();
                return true;
            case 1005: // 按颜色筛选
                showColorFilterDialog();
                return true;
            case 1006: // 清除筛选
                clearSearch();
                return true;
            case 1007: // 按颜色排序
                mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_COLOR + " ASC, " +
                        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";
                refreshList();
                return true;
            case 1008: // 搜索笔记
                showSearchDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("搜索笔记");

        final EditText input = new EditText(this);
        input.setHint("输入标题或内容关键字");
        builder.setView(input);

        builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentSearchQuery = input.getText().toString().trim();
                refreshList();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showCategoryFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择分类");

        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[] { "DISTINCT " + NotePad.Notes.COLUMN_NAME_CATEGORY },
                NotePad.Notes.COLUMN_NAME_CATEGORY + " IS NOT NULL AND "
                        + NotePad.Notes.COLUMN_NAME_CATEGORY + " != ''",
                null,
                NotePad.Notes.COLUMN_NAME_CATEGORY + " ASC"
        );

        final List<String> categories = new ArrayList<>();
        categories.add("(全部)");
        categories.add("(新建分类...)");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String category = cursor.getString(0);
                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
            }
            cursor.close();
        }

        final CharSequence[] items = categories.toArray(new CharSequence[0]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mCurrentCategoryFilter = "";
                    refreshList();
                } else if (which == 1) {
                    showNewCategoryDialog();
                } else {
                    mCurrentCategoryFilter = items[which].toString();
                    refreshList();
                }
            }
        });

        builder.show();
    }

    private void showNewCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新建分类");

        final EditText input = new EditText(this);
        input.setHint("输入分类名称");
        builder.setView(input);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newCategory = input.getText().toString().trim();
                if (!newCategory.isEmpty()) {
                    mCurrentCategoryFilter = newCategory;
                    refreshList();
                }
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showColorFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择颜色");

        final String[] colors = {"全部", "默认(白色)", "红色", "蓝色", "绿色", "黄色", "紫色"};
        final int[] colorValues = {-1, NotePad.Notes.COLOR_DEFAULT, NotePad.Notes.COLOR_RED,
                NotePad.Notes.COLOR_BLUE, NotePad.Notes.COLOR_GREEN,
                NotePad.Notes.COLOR_YELLOW, NotePad.Notes.COLOR_PURPLE};

        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentColorFilter = colorValues[which];
                refreshList();
            }
        });

        builder.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        int id = item.getItemId();
        if (id == R.id.context_open) {
            // 改为显式Intent
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(noteUri);
            startActivity(intent);
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newUri(
                    getContentResolver(),
                    "Note",
                    noteUri));
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(
                    noteUri,
                    null,
                    null
            );
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // 改为显式Intent
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(uri);
            startActivity(intent);
        }
    }
}