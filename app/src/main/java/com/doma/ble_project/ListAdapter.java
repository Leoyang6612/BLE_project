package com.doma.ble_project;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter
{
    private LayoutInflater mLayInf;
    private ArrayList<String> arrayList=new ArrayList();
    public ListAdapter(Context context, ArrayList<String> input)
    {
        mLayInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        arrayList = input;
    }

    @Override
    public int getCount()
    {
        //取得 ListView 列表 Item 的數量
        return arrayList.size();
    }

    @Override
    public Object getItem(int position)
    {
        //取得 ListView 列表於 position 位置上的 Item
        return position;
    }

    @Override
    public long getItemId(int position)
    {
        //取得 ListView 列表於 position 位置上的 Item 的 ID
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        //設定與回傳 convertView 作為顯示在這個 position 位置的 Item 的 View。
        View v = mLayInf.inflate(R.layout.list_view_item, parent, false);


        TextView textViewName = (TextView) v.findViewById(R.id.textview_name);
        TextView textViewLeft = (TextView) v.findViewById(R.id.textview_left);
        TextView textViewRight = (TextView) v.findViewById(R.id.textview_right);
        TextView textViewRead = (TextView) v.findViewById(R.id.textview_read);
        //TextView textViewBottom =  (TextView) v.findViewById(R.id.textview_bottom);
        ImageView imageViewLeft=(ImageView)v.findViewById(R.id.icon_left);
        ImageView imageViewRight=(ImageView)v.findViewById(R.id.icon_right);

        String[] split=arrayList.get(position).toString().split("&");
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.linearlayout);
        LinearLayout infoLayout = (LinearLayout) v.findViewById(R.id.information_layout);


        if(split[0].equals("me")){
            layout.setGravity(Gravity.RIGHT);
            infoLayout.setGravity(Gravity.RIGHT);

            if(split[5].equals("Read")){
                //textViewLeft.setTextColor(Color.RED);
                Log.d("TAG","已讀!!!");
                //textViewLeft.setText("readed " + split[1] );
                textViewLeft.setText(split[1]);
                textViewName.setText(split[0]);
                textViewRead.setText("Read");
            }
            else {
                textViewLeft.setText(split[1]);
                textViewName.setText(split[0]);

            }

            if(split[3].equals("1")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_one));
            }
            else if(split[3].equals("2")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_two));
            }
            else if(split[3].equals("3")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_three));
            }
            else if(split[3].equals("4")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_four));
            }
            else if(split[3].equals("5")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_five));
            }
            else if(split[3].equals("6")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_six));
            }
            else if(split[3].equals("7")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_seven));
            }
            else if(split[3].equals("8")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_eight));
            }
            else if(split[3].equals("9")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_nine));
            }
            else if(split[3].equals("0")){
                imageViewRight.setBackground(v.getResources().getDrawable(R.drawable.id_zero));
            }



            textViewRight.setGravity(Gravity.RIGHT);
            textViewRight.setBackground(v.getResources().getDrawable(R.drawable.bubble_right));
            textViewRight.setTextSize(16);
            textViewRight.setText(split[2]+"  ");
            imageViewRight.setVisibility(View.VISIBLE);


        }
        else{
            layout.setGravity(Gravity.LEFT);

            textViewName.setText(split[0]);
            textViewRight.setText(split[1]);
            textViewLeft.setBackground(v.getResources().getDrawable(R.drawable.bubble_left));
            textViewLeft.setTextSize(16);
            textViewLeft.setText("   "+split[2]);

            Log.d("TAG","split[3]: "+split[3]);
            Log.d("TAG","split[4]: "+split[4]);

            if(split[4].equals("1")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_one));
            }
            else if(split[4].equals("2")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_two));
            }
            else if(split[4].equals("3")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_three));
            }
            else if(split[4].equals("4")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_four));
            }
            else if(split[4].equals("5")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_five));
            }
            else if(split[4].equals("6")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_six));
            }
            else if(split[4].equals("7")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_seven));
            }
            else if(split[4].equals("8")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_eight));
            }
            else if(split[4].equals("9")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_nine));
            }
            else if(split[4].equals("0")){
                imageViewLeft.setBackground(v.getResources().getDrawable(R.drawable.id_zero));
            }

            imageViewLeft.setVisibility(View.VISIBLE);
        }




        return v;
    }
}
