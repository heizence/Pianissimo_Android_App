package com.example.pianissimo.Adapters;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_payment_history extends RecyclerView.Adapter<Adapter_each_payment_history.ViewHolder> {
    public Context context;

    // 결제 내역 데이터
    private List<Part_each_payment_history> data = new ArrayList<>();

    // adapter 생성
    public Adapter_each_payment_history(Context context) {
        this.context = context;
    }

    // 데이터 추가
    public void add(Part_each_payment_history object) {
        data.add(object);
        this.notifyItemInserted(data.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView statusView;
        private final TextView ticketTypeView;
        private final TextView startDateView;
        private final TextView endDateView;

        public ViewHolder(View view) {
            super(view);
            statusView = view.findViewById(R.id.eachPaymentHistoryStatus);
            ticketTypeView = view.findViewById(R.id.eachPaymentHistoryTicketType);
            startDateView = view.findViewById(R.id.eachPaymentHistoryTicketStartDate);
            endDateView = view.findViewById(R.id.eachPaymentHistoryTicketEndDate);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Boolean isActive = data.get(position).getIsActive();
        if (isActive) viewHolder.statusView.setText("이용중");
        else {
            viewHolder.statusView.setText("만료");
            viewHolder.statusView.setTextColor(context.getResources().getColor(R.color.red_001));
        }

        viewHolder.ticketTypeView.setText(data.get(position).getTicketName());
        viewHolder.startDateView.setText("결제일 : " + data.get(position).getStartDate());
        viewHolder.endDateView.setText("만료일 : " + data.get(position).getEndDate());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
