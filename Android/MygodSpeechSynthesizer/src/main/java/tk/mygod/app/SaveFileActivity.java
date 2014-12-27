package tk.mygod.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.*;
import tk.mygod.speech.synthesizer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class SaveFileActivity extends ActionBarActivity
        implements AdapterView.OnItemClickListener, Comparator<File> {
    public static final String EXTRA_CURRENT_DIRECTORY = "tk.mygod.intent.extra.CurrentDirectory";
    private String mimeType;
    private File currentDirectory;
    private ArrayList<File> directoryList;
    private EditText fileName;
    private ListView directoryView;

    private void setCurrentDirectory(File directory) {
        if (directory != null) {
            currentDirectory = directory;
            String path = currentDirectory.getAbsolutePath();
            if (currentDirectory.getParent() != null) path += "/";
            getSupportActionBar().setSubtitle(path);
        }
        directoryList = new ArrayList<>();
        File[] files = currentDirectory.listFiles();
        Arrays.sort(files, this);
        if (currentDirectory.getParent() != null) directoryList.add(new File(".."));
        if (files != null) for (File file : files) if (file.isDirectory() || file.isFile() && mimeType
                .equals(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap
                        .getFileExtensionFromUrl(file.getAbsolutePath())))) directoryList.add(file);
        directoryView.setAdapter(new DirectoryDisplay(this, directoryList));
    }

    public void submit(View view) {
        if (new File(currentDirectory, fileName.getText().toString()).exists())
            new AlertDialog.Builder(this).setTitle(R.string.dialog_overwrite_confirm_title)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            confirm();
                        }
                    }).setNegativeButton(android.R.string.no, null).show();
        else confirm();
    }

    private void confirm() {
        Intent result = new Intent();
        result.setData(Uri.fromFile(new File(currentDirectory, fileName.getText().toString())));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        (directoryView = (ListView) findViewById(R.id.directory_view)).setOnItemClickListener(this);
        fileName = (EditText) findViewById(R.id.file_name);
        Intent intent = getIntent();
        mimeType = intent.getType();
        String path = intent.getStringExtra(EXTRA_CURRENT_DIRECTORY),
               defaultFileName = intent.getStringExtra(Intent.EXTRA_TITLE);
        setCurrentDirectory(path == null ? Environment.getExternalStorageDirectory() : new File(path));
        if (defaultFileName != null) fileName.setText(defaultFileName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_file_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_create_dir:
                final EditText text = new EditText(this);
                new AlertDialog.Builder(this).setTitle(R.string.dialog_create_dir_title).setView(text)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (new File(currentDirectory, text.getText().toString()).mkdirs())
                                    setCurrentDirectory(null);
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 && position >= directoryList.size()) return;
        File selected = directoryList.get(position);
        if (selected.isFile()) {
            fileName.setText(selected.getName());
            submit(null);
        }
        else setCurrentDirectory("..".equals(selected.getName()) ? currentDirectory.getParentFile() : selected);
    }

    @Override
    public int compare(File lhs, File rhs) {
        int result = ((Boolean) lhs.isFile()).compareTo(rhs.isFile());
        return result == 0 && (result = lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase())) == 0
                ? lhs.getName().compareTo(rhs.getName()) : result;
    }

    private class DirectoryDisplay extends ArrayAdapter<File> {
        public DirectoryDisplay(Context context, List<File> displayContent) {
            super(context, android.R.layout.activity_list_item, android.R.id.text1, displayContent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = super.getView(position, null, parent);
            File selected = directoryList.get(position);
            if (selected != null) {
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(selected.getName());
                ((ImageView) convertView.findViewById(android.R.id.icon)).setImageResource
                        (selected.isFile() ? R.drawable.ic_doc_generic_am_alpha : R.drawable.ic_doc_folder_alpha);
            }
            return convertView;
        }
    }
}
