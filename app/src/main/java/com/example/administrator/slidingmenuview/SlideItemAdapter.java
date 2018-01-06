package com.example.administrator.slidingmenuview;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Administrator on 2018/1/6.
 */

public class SlideItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements  ItemSlideHelper.Callback {

    private RecyclerView mRecyclerView;
    private TextVH holder;

    class TextVH extends RecyclerView.ViewHolder {

        TextView textView, tx_delete;
        public TextVH(final View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.tv_text);
            tx_delete = (TextView) itemView.findViewById(R.id.tv_delete);


        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slide, parent, false);
        holder = new TextVH(view);

        holder.tx_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyItemRemoved(holder.getAdapterPosition());
            }
        });
        return new TextVH(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        String text = "item:" + position;
        TextVH textVH = (TextVH) holder;
        textVH.textView.setText(text);



    }

    @Override
    public int getItemCount() {
        return 20;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
        mRecyclerView.addOnItemTouchListener(new ItemSlideHelper(mRecyclerView.getContext(), this));
    }

    @Override
    public int getHorizontalRange(RecyclerView.ViewHolder holder) {
        if (holder.itemView instanceof LinearLayout) {
            ViewGroup viewGroup = (ViewGroup) holder.itemView;
            if (viewGroup.getChildCount() == 2){
                return viewGroup.getChildAt(1).getLayoutParams().width;
            }
        }
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder getChildViewHoder(View childView) {
        return mRecyclerView.getChildViewHolder(childView);
    }

    @Override
    public View findTargetView(float x, float y) {
        return mRecyclerView.findChildViewUnder(x, y);
    }
}
