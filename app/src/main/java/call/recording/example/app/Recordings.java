package call.recording.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.axet.androidlibrary.widgets.ErrorDialog;


import java.util.ArrayList;
import java.util.TreeSet;

import call.recording.example.R;

public class Recordings extends com.github.axet.audiolibrary.app.Recordings {
    protected View toolbar_i;
    protected View toolbar_o;
    View refresh;
    public TextView progressText;
    public View progressEmpty;

    boolean toolbarFilterIn;
    boolean toolbarFilterOut;

    public Recordings(Context context, ListView list) {
        super(context, list);
        View empty = list.getEmptyView();
        refresh = empty.findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load(false, null);
            }
        });
        progressText = (TextView) empty.findViewById(android.R.id.text1);
        progressEmpty = empty.findViewById(R.id.progress_empty);
    }

    @Override
    public void load(Uri mount, boolean clean, Runnable done) {
        progressText.setText(R.string.recording_list_is_empty);
        refresh.setVisibility(View.GONE);
        if (!com.github.axet.audiolibrary.app.Storage.exists(getContext(), mount)) { // folder may not exist, do not show error
            scan(new ArrayList<com.github.axet.audiolibrary.app.Storage.Node>(), clean, done);
            return;
        }
        try {
            super.load(mount, clean, done);
        } catch (RuntimeException e) {
            Log.e(TAG, "unable to load", e);
            refresh.setVisibility(View.VISIBLE);
            progressText.setText(ErrorDialog.toMessage(e));
            scan(new ArrayList<com.github.axet.audiolibrary.app.Storage.Node>(), clean, done);
        }
    }

    @Override
    public String[] getEncodingValues() {
        return Storage.getEncodingValues(getContext());
    }

    @Override
    public void cleanDelete(TreeSet<String> delete, Uri f) {
        super.cleanDelete(delete, f);
        String p = CallApplication.getFilePref(f);
        delete.remove(p + CallApplication.PREFERENCE_DETAILS_CONTACT);
        delete.remove(p + CallApplication.PREFERENCE_DETAILS_CALL);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        LinearLayout s = (LinearLayout) v.findViewById(R.id.recording_status);
        ImageView i = (ImageView) v.findViewById(R.id.recording_call);
        Storage.RecordingUri u = getItem(position);
        String call = CallApplication.getCall(getContext(), u.uri);
        if (call == null || call.isEmpty()) {
            i.setVisibility(View.GONE);
        } else {
            switch (call) {
                case CallApplication.CALL_IN:
                    i.setVisibility(View.VISIBLE);
                    i.setImageResource(R.drawable.ic_call_received_black_24dp);
                    break;
                case CallApplication.CALL_OUT:
                    i.setVisibility(View.VISIBLE);
                    i.setImageResource(R.drawable.ic_call_made_black_24dp);
                    break;
            }
        }
        return v;
    }

    @Override
    protected boolean filter(Storage.RecordingUri f) {
        boolean include = super.filter(f);
        if (include) {
            if (!toolbarFilterIn && !toolbarFilterOut)
                return true;
            String call = CallApplication.getCall(getContext(), f.uri);
            if (call == null || call.isEmpty())
                return false;
            if (toolbarFilterIn)
                return call.equals(CallApplication.CALL_IN);
            if (toolbarFilterOut)
                return call.equals(CallApplication.CALL_OUT);
        }
        return include;
    }

    public void setToolbar(ViewGroup v) {
        toolbar_i = v.findViewById(R.id.toolbar_in);
        toolbar_i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarFilterIn = !toolbarFilterIn;
                if (toolbarFilterIn)
                    toolbarFilterOut = false;
                selectToolbar();
                load(false, null);
                save();
            }
        });
        toolbar_o = v.findViewById(R.id.toolbar_out);
        toolbar_o.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarFilterOut = !toolbarFilterOut;
                if (toolbarFilterOut)
                    toolbarFilterIn = false;
                selectToolbar();
                load(false, null);
                save();
            }
        });
        super.setToolbar(v);
    }

    protected void selectToolbar() {
        super.selectToolbar();
        selectToolbar(toolbar_i, toolbarFilterIn);
        selectToolbar(toolbar_o, toolbarFilterOut);
    }

    protected void save() {
        super.save();
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = shared.edit();
        edit.putBoolean(CallApplication.PREFERENCE_FILTER_IN, toolbarFilterIn);
        edit.putBoolean(CallApplication.PREFERENCE_FILTER_OUT, toolbarFilterOut);
        edit.commit();
    }

    protected void load() {
        super.load();
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
        toolbarFilterIn = shared.getBoolean(CallApplication.PREFERENCE_FILTER_IN, false);
        toolbarFilterOut = shared.getBoolean(CallApplication.PREFERENCE_FILTER_OUT, false);
    }
}
