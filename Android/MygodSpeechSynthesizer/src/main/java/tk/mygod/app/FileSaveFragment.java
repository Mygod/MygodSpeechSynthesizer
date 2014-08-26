package tk.mygod.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import tk.mygod.speech.synthesizer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author   Mygod
 * Based on: http://www.codeproject.com/Articles/704643/An-Android-File-Save-Dialogue
 */
public class FileSaveFragment extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String PARENT = "..";

    private Callbacks callbacks;
    private ArrayList<File> directoryList;
    private int icon;
    private String defaultFileName;

    private TextView currentPath;
    private EditText fileName;
    private ListView directoryView;

    private File currentDirectory;

    public void setIcon(int value) {
        icon = value;
    }
    public void setDefaultFileName(String value) {
        defaultFileName = value;
    }

    public static FileSaveFragment newInstance(Callbacks value) {
        FileSaveFragment result = new FileSaveFragment();
        result.callbacks = value;
        return result;
    }

    /**
     * Signal to / request action of host activity.
     */
    public interface Callbacks {
        /**
         * Hand potential file details to context for validation.
         *
         * @param absolutePath - Absolute path to target directory.
         * @param fileName     - Filename. Not guaranteed to have a type extension.
         */
        public boolean onCanSave(String absolutePath, String fileName);

        /**
         * Hand validated path and name to context for use.
         * If user cancels absolutePath and filename are handed out as null.
         *
         * @param absolutePath - Absolute path to target directory.
         * @param fileName     - Filename. Not guaranteed to have a type extension.
         */
        public void onConfirmSave(String absolutePath, String fileName);
    }

    /**
     * Note the parent activity for callback purposes.
     *
     * @param activity - parent activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Callbacks) callbacks = (Callbacks) activity;
        directoryList = new ArrayList<File>();
    }

    /**
     * Build the popup.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View root = getActivity().getLayoutInflater().inflate(R.layout.fragment_file_save, null);
        currentDirectory = Environment.getExternalStorageDirectory();
        directoryList = getSubDirectories(currentDirectory);
        DirectoryDisplay displayFormat = new DirectoryDisplay(getActivity(), directoryList);
        (directoryView = (ListView) root.findViewById(R.id.directory_view)).setAdapter(displayFormat);
        directoryView.setOnItemClickListener(this);
        (currentPath = (TextView) root.findViewById(R.id.current_path))
                .setText(currentDirectory.getAbsolutePath() + "/");
        fileName = (EditText) root.findViewById(R.id.file_name);
        if (defaultFileName != null) fileName.setText(defaultFileName);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(root);
        if (icon != 0) builder.setIcon(icon);
        Dialog result = builder.setTitle(R.string.file_save_title)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String absolutePath = currentDirectory.getAbsolutePath();
                                String filename = fileName.getText().toString();
                                if (callbacks.onCanSave(absolutePath, filename)) {
                                    dismiss();
                                    callbacks.onConfirmSave(absolutePath, filename);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            callbacks.onConfirmSave(null, null);
                        }
                    }).create();
        result.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return result;
    }

    /**
     * Identify all sub-directories within a directory.
     *
     * @param directory The directory to walk.
     */
    private ArrayList<File> getSubDirectories(File directory) {
        ArrayList<File> directories = new ArrayList<File>();
        File[] files = directory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        if (directory.getParent() != null) directories.add(new File(PARENT));
        if (files != null) for (File f : files) if (f.isDirectory()) directories.add(f);
        return directories;
    }

    /**
     * Refresh the listview's display adapter using the content
     * of the identified directory.
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View list, int pos, long id) {
        if (pos < 0 && pos >= directoryList.size()) return;
        File selected = directoryList.get(pos);
        String name = selected.getName();
        currentDirectory = name.equals(PARENT) ? currentDirectory.getParentFile() : selected;
        directoryList = getSubDirectories(currentDirectory);
        DirectoryDisplay displayFormatter = new DirectoryDisplay(getActivity(), directoryList);
        directoryView.setAdapter(displayFormatter);
        String path = currentDirectory.getAbsolutePath();
        if (currentDirectory.getParent() != null) path += "/";
        currentPath.setText(path);
    }

    /**
     * Display the sub-directories in a selected directory.
     */
    private class DirectoryDisplay extends ArrayAdapter<File> {
        public DirectoryDisplay(Context context, List<File> displayContent) {
            super(context, android.R.layout.simple_list_item_1, displayContent);
        }

        /**
         * Display the name of each sub-directory.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textview = (TextView) super.getView(position, convertView, parent);
            if (directoryList.get(position) != null) textview.setText(directoryList.get(position).getName());
            return textview;
        }
    }
}
