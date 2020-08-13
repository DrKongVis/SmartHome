package com.example.smart_home_terminal.TaskList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.smart_home_terminal.R;

import java.util.List;

public class TaskAdapter extends BaseAdapter {

    private List<TaskClass> tasks;
    private Context context;

    public TaskAdapter(List<TaskClass> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.task_list_item,null);

        TextView name = convertView.findViewById(R.id.list_task_name);
        TextView start = convertView.findViewById(R.id.list_task_startTime);
        TextView end = convertView.findViewById(R.id.list_task_endTime);

        TaskClass user = tasks.get(position);
        name.setText(user.getName());
        start.setText(user.getStartTime());
        end.setText(user.getEndTime());

        return convertView;
    }
}
