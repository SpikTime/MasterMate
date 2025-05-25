package com.example.mastermate.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private static final String TAG = "OrderAdapter";
    private Context context;
    androidx.constraintlayout.widget.ConstraintLayout actionButtonsLayoutClient;
    private List<Order> orderList;
    private String userRole;
    private OrderActionListener actionListener;

    public interface OrderActionListener {
        void onAcceptOrder(Order order);

        void onRejectOrder(Order order);

        void onCompleteOrder(Order order);



        void onCallClient(String phoneNumber);

        void onRateClient(Order order);

        void onConfirmCompletionByClient(Order order);

        void onOpenDisputeByClient(Order order);

        void onLeaveReviewByClient(Order order);

        void onCallMaster(String phoneNumber);

        void onItemClick(Order order);
    }

    public OrderAdapter(Context context, List<Order> orderList, String userRole, OrderActionListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.userRole = userRole;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if ("master".equals(userRole)) {
            view = LayoutInflater.from(context).inflate(R.layout.item_order_master, parent, false);
        } else { // "client"
            view = LayoutInflater.from(context).inflate(R.layout.item_order_client, parent, false);
        }
        return new OrderViewHolder(view, userRole);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (orderList == null || position < 0 || position >= orderList.size()) {
            Log.e(TAG, "Invalid position or orderList is null/empty. Position: " + position);
            if (holder.itemView != null) holder.itemView.setVisibility(View.GONE);
            return;
        }
        Order order = orderList.get(position);
        if (order == null) {
            Log.w(TAG, "Order object at position " + position + " is null. Hiding item.");
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Binding order ID: " + order.getOrderId() + " for role: " + userRole + " at pos: " + position);

        TextView problemDescTextViewToUse = null;
        TextView dateTextViewToUse = null;
        TextView statusTextViewToUse = null;

        if ("master".equals(userRole)) {
            problemDescTextViewToUse = holder.problemDescTextViewMaster;
            dateTextViewToUse = holder.dateTextViewMaster;
            statusTextViewToUse = holder.statusTextViewMaster;
            if (holder.clientNameTextView != null)
                holder.clientNameTextView.setText(order.getClientName() != null ? order.getClientName() : "Клиент не указан");
            if (holder.addressTextViewMaster != null)
                holder.addressTextViewMaster.setText(order.getClientAddress() != null ? order.getClientAddress() : "Адрес не указан");

            if (holder.clientRatingContainer != null && holder.orderClientRatingLabelTextView != null &&
                    holder.orderClientRatingBar != null && holder.orderClientRatingCountTextView != null) {
                if (Order.STATUS_NEW.equals(order.getStatus()) && order.getClientId() != null) {
                    holder.clientRatingContainer.setVisibility(View.GONE);
                    DatabaseReference clientRef = FirebaseDatabase.getInstance().getReference("users").child(order.getClientId());
                    clientRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (holder.getAdapterPosition() != position || !snapshot.exists() || holder.clientRatingContainer == null)
                                return;
                            Double avgRating = snapshot.child("clientAverageRating").getValue(Double.class);
                            Long ratedCount = snapshot.child("clientRatedByMastersCount").getValue(Long.class);
                            if (ratedCount != null && ratedCount > 0 && avgRating != null && avgRating > 0) {
                                holder.orderClientRatingLabelTextView.setText("Рейтинг клиента:");
                                holder.orderClientRatingBar.setRating(avgRating.floatValue());
                                holder.orderClientRatingCountTextView.setText("(" + ratedCount + ")");
                                holder.clientRatingContainer.setVisibility(View.VISIBLE);
                            } else {
                                holder.orderClientRatingLabelTextView.setText("Рейтинг клиента: нет оценок");
                                holder.orderClientRatingBar.setVisibility(View.GONE);
                                holder.orderClientRatingCountTextView.setText("");
                                holder.clientRatingContainer.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (holder.getAdapterPosition() == position && holder.clientRatingContainer != null)
                                holder.clientRatingContainer.setVisibility(View.GONE);
                        }
                    });
                } else {
                    holder.clientRatingContainer.setVisibility(View.GONE);
                }
            }
            setupMasterActionButtons(holder, order);
        } else if ("client".equals(userRole)) {
            problemDescTextViewToUse = holder.problemDescTextViewClient;
            dateTextViewToUse = holder.dateTextViewClient;
            statusTextViewToUse = holder.statusTextViewClient;
            if (holder.masterNameTextView != null)
                holder.masterNameTextView.setText(order.getMasterName() != null ? "Мастер: " + order.getMasterName() : "Мастер не указан");
            if (holder.addressTextViewClient != null) {
                if (order.getClientAddress() != null && !order.getClientAddress().isEmpty()) {
                    holder.addressTextViewClient.setText(order.getClientAddress());
                    holder.addressTextViewClient.setVisibility(View.VISIBLE);
                } else {
                    holder.addressTextViewClient.setVisibility(View.GONE);
                }
            }
            setupClientActionButtons(holder, order);
        }

        if (problemDescTextViewToUse != null)
            problemDescTextViewToUse.setText(order.getProblemDescription() != null ? order.getProblemDescription() : "Описание отсутствует");
        if (dateTextViewToUse != null) {
            long timestamp = order.getCreationTimestampLong();
            if (timestamp > 0) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
                    dateTextViewToUse.setText(sdf.format(new Date(timestamp)));
                } catch (Exception e) {
                    dateTextViewToUse.setText("Неверная дата");
                }
            } else {
                dateTextViewToUse.setText("Дата не указана");
            }
        }
        if (statusTextViewToUse != null) setupStatus(statusTextViewToUse, order.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                Log.d(TAG, "itemView clicked for order: " + order.getOrderId() + " by role: " + userRole); // Добавь лог
                actionListener.onItemClick(order);
            } else {
                Log.e(TAG, "actionListener is NULL in onBindViewHolder!");
            }
        });
    }

    private void setupStatus(TextView statusTextView, String status) {
        if (statusTextView == null || status == null) return;
        String statusTextDisplay = status;
        int backgroundColorRes = R.color.chip_default_background_light;
        int textColorRes = R.color.chip_default_text_light;

        switch (status.toLowerCase()) {
            case Order.STATUS_NEW:
                statusTextDisplay = "Новый";
                backgroundColorRes = R.color.chip_status_new_background;
                textColorRes = R.color.chip_status_new_text;
                break;
            case Order.STATUS_ACCEPTED:
                statusTextDisplay = "Принят";
                backgroundColorRes = R.color.chip_status_accepted_background;
                textColorRes = R.color.chip_status_accepted_text;
                break;
            case Order.STATUS_IN_PROGRESS:
                statusTextDisplay = "В работе";
                backgroundColorRes = R.color.chip_status_inprogress_background;
                textColorRes = R.color.chip_status_inprogress_text;
                break;
            case Order.STATUS_COMPLETED_MASTER:
                statusTextDisplay = "Завершен вами";
                backgroundColorRes = R.color.chip_status_completed_master_background;
                textColorRes = R.color.chip_status_completed_master_text;
                break;
            case Order.STATUS_CONFIRMED_CLIENT:
                statusTextDisplay = "Выполнен";
                backgroundColorRes = R.color.chip_status_confirmed_client_background;
                textColorRes = R.color.chip_status_confirmed_client_text;
                break;
            case Order.STATUS_REJECTED_MASTER:
                statusTextDisplay = "Отклонен вами";
                backgroundColorRes = R.color.chip_status_rejected_background;
                textColorRes = R.color.chip_status_rejected_text;
                break;
            case Order.STATUS_CANCELLED_CLIENT:
                statusTextDisplay = "Отменен клиентом";
                backgroundColorRes = R.color.chip_status_cancelled_background;
                textColorRes = R.color.chip_status_cancelled_text;
                break;
            case Order.STATUS_DISPUTED:
                statusTextDisplay = "Спор";
                backgroundColorRes = R.color.chip_status_disputed_background;
                textColorRes = R.color.chip_status_disputed_text;
                break;
            default:
                statusTextDisplay = status;
                break;
        }
        statusTextView.setText(statusTextDisplay);
        try {
            statusTextView.setTextColor(ContextCompat.getColor(context, textColorRes));
            Drawable background = statusTextView.getBackground();
            if (background instanceof GradientDrawable)
                ((GradientDrawable) background.mutate()).setColor(ContextCompat.getColor(context, backgroundColorRes));
            else
                statusTextView.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, backgroundColorRes)));
        } catch (Resources.NotFoundException e) {
            statusTextView.setTextColor(Color.BLACK);
            statusTextView.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void setupMasterActionButtons(OrderViewHolder holder, Order order) {
        LinearLayout masterButtonContainer = holder.actionButtonsLayoutMaster;
        if (masterButtonContainer == null) {
            Log.e(TAG, "Master button container is null.");
            return;
        }

        if (holder.acceptButton != null) holder.acceptButton.setVisibility(View.GONE);
        if (holder.rejectButton != null) holder.rejectButton.setVisibility(View.GONE);
        if (holder.callClientButton != null) holder.callClientButton.setVisibility(View.GONE);
        if (holder.completeButton != null) holder.completeButton.setVisibility(View.GONE);
        if (holder.rateClientButton != null) holder.rateClientButton.setVisibility(View.GONE);
        masterButtonContainer.setVisibility(View.GONE);

        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        boolean showAnyButton = false;

        if (Order.STATUS_NEW.equals(status)) {
            if (holder.acceptButton != null) holder.acceptButton.setVisibility(View.VISIBLE);
            if (holder.rejectButton != null) holder.rejectButton.setVisibility(View.VISIBLE);
            if (holder.callClientButton != null && order.getClientPhoneNumber() != null && !order.getClientPhoneNumber().isEmpty())
                holder.callClientButton.setVisibility(View.VISIBLE);
            showAnyButton = true;
        } else if (Order.STATUS_ACCEPTED.equals(status) || Order.STATUS_IN_PROGRESS.equals(status)) {
            if (holder.completeButton != null) {
                holder.completeButton.setText("Завершить заказ");
                holder.completeButton.setVisibility(View.VISIBLE);
            }
            if (holder.callClientButton != null && order.getClientPhoneNumber() != null && !order.getClientPhoneNumber().isEmpty())
                holder.callClientButton.setVisibility(View.VISIBLE);
            showAnyButton = true;
        } else if (Order.STATUS_COMPLETED_MASTER.equals(status)) {
            if (holder.rateClientButton != null && order.getMasterRatingForClient() == 0f) {
                holder.rateClientButton.setVisibility(View.VISIBLE);
                showAnyButton = true;
            }
            if (holder.callClientButton != null && order.getClientPhoneNumber() != null && !order.getClientPhoneNumber().isEmpty() && !showAnyButton) {
                holder.callClientButton.setVisibility(View.VISIBLE);
                showAnyButton = true;
            }
        } else if (Order.STATUS_CONFIRMED_CLIENT.equals(status)) {
            if (holder.rateClientButton != null && order.getMasterRatingForClient() == 0f) {
                holder.rateClientButton.setVisibility(View.VISIBLE);
                showAnyButton = true;
            }
        }
        masterButtonContainer.setVisibility(showAnyButton ? View.VISIBLE : View.GONE);

        if (holder.acceptButton != null) holder.acceptButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAcceptOrder(order);
        });
        if (holder.rejectButton != null) holder.rejectButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onRejectOrder(order);
        });
        if (holder.callClientButton != null) holder.callClientButton.setOnClickListener(v -> {
            if (actionListener != null && order.getClientPhoneNumber() != null)
                actionListener.onCallClient(order.getClientPhoneNumber());
        });
        if (holder.completeButton != null) holder.completeButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onCompleteOrder(order);
        });
        if (holder.rateClientButton != null) holder.rateClientButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onRateClient(order);
        });
    }

    private void setupClientActionButtons(OrderViewHolder holder, Order order) {
        ConstraintLayout clientButtonContainer = holder.actionButtonsLayoutClient;
        if (clientButtonContainer == null) {
            Log.e(TAG, "Client button container is null.");
            return;
        }

        if (holder.btnConfirmCompletionClient != null)
            holder.btnConfirmCompletionClient.setVisibility(View.GONE);
        if (holder.btnOpenDisputeClient != null)
            holder.btnOpenDisputeClient.setVisibility(View.GONE);
        if (holder.btnLeaveReviewClient != null)
            holder.btnLeaveReviewClient.setVisibility(View.GONE);
        if (holder.btnCallMasterClient != null) holder.btnCallMasterClient.setVisibility(View.GONE);
        clientButtonContainer.setVisibility(View.GONE);

        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        boolean showAnyButton = false;

        if (Order.STATUS_COMPLETED_MASTER.equals(status)) {
            if (holder.btnConfirmCompletionClient != null)
                holder.btnConfirmCompletionClient.setVisibility(View.VISIBLE);
            if (holder.btnOpenDisputeClient != null)
                holder.btnOpenDisputeClient.setVisibility(View.VISIBLE);
            showAnyButton = true;
        } else if (Order.STATUS_CONFIRMED_CLIENT.equals(status)) {
            // boolean reviewNotLeft = !order.isClientReviewLeft(); // TODO: Реализовать проверку флага отзыва
            boolean reviewNotLeft = true;
            if (holder.btnLeaveReviewClient != null && reviewNotLeft) {
                holder.btnLeaveReviewClient.setVisibility(View.VISIBLE);
                showAnyButton = true;
            }
        } else if (Order.STATUS_NEW.equals(status) || Order.STATUS_ACCEPTED.equals(status) || Order.STATUS_IN_PROGRESS.equals(status)) {
            if (holder.btnCallMasterClient != null && order.getMasterPhoneNumber() != null && !order.getMasterPhoneNumber().isEmpty()) {
                holder.btnCallMasterClient.setVisibility(View.VISIBLE);
                showAnyButton = true;
            }
        }
        clientButtonContainer.setVisibility(showAnyButton ? View.VISIBLE : View.GONE);

        if (holder.btnConfirmCompletionClient != null)
            holder.btnConfirmCompletionClient.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onConfirmCompletionByClient(order);
            });
        if (holder.btnOpenDisputeClient != null)
            holder.btnOpenDisputeClient.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onOpenDisputeByClient(order);
            });
        if (holder.btnLeaveReviewClient != null)
            holder.btnLeaveReviewClient.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onLeaveReviewByClient(order);
            });
        if (holder.btnCallMasterClient != null) holder.btnCallMasterClient.setOnClickListener(v -> {
            if (actionListener != null && order.getMasterPhoneNumber() != null)
                actionListener.onCallMaster(order.getMasterPhoneNumber());
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateOrders(List<Order> newOrders) {
        this.orderList.clear();
        if (newOrders != null) {
            this.orderList.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView clientNameTextView, addressTextViewMaster, problemDescTextViewMaster, dateTextViewMaster, statusTextViewMaster;
        LinearLayout actionButtonsLayoutMaster;
        Button acceptButton, rejectButton, callClientButton, completeButton, rateClientButton;
        LinearLayout clientRatingContainer;
        TextView orderClientRatingLabelTextView;
        RatingBar orderClientRatingBar;
        TextView orderClientRatingCountTextView;

        TextView masterNameTextView, problemDescTextViewClient, dateTextViewClient, statusTextViewClient;
        TextView addressTextViewClient;
        androidx.constraintlayout.widget.ConstraintLayout actionButtonsLayoutClient;
        Button btnConfirmCompletionClient, btnOpenDisputeClient, btnLeaveReviewClient, btnCallMasterClient;

        public OrderViewHolder(@NonNull View itemView, String role) {
            super(itemView);

            Log.d(TAG, "OrderViewHolder created for role: " + role);

            if ("master".equals(role)) {
                Log.d(TAG, "Initializing views for MASTER role.");
                statusTextViewMaster = itemView.findViewById(R.id.orderStatusTextView);
                clientNameTextView = itemView.findViewById(R.id.orderClientNameTextView);
                dateTextViewMaster = itemView.findViewById(R.id.orderDateTextView);
                problemDescTextViewMaster = itemView.findViewById(R.id.orderProblemDescTextView);
                addressTextViewMaster = itemView.findViewById(R.id.orderAddressTextView);
                actionButtonsLayoutMaster = itemView.findViewById(R.id.orderActionButtonsLayout);
                acceptButton = itemView.findViewById(R.id.orderAcceptButton);
                rejectButton = itemView.findViewById(R.id.orderRejectButton);
                callClientButton = itemView.findViewById(R.id.orderCallClientButton);
                completeButton = itemView.findViewById(R.id.orderCompleteButton);
                rateClientButton = itemView.findViewById(R.id.orderRateClientButton);

                clientRatingContainer = itemView.findViewById(R.id.clientRatingContainer);
                orderClientRatingLabelTextView = itemView.findViewById(R.id.orderClientRatingLabelTextView);
                orderClientRatingBar = itemView.findViewById(R.id.orderClientRatingBar);
                orderClientRatingCountTextView = itemView.findViewById(R.id.orderClientRatingCountTextView);

                if (actionButtonsLayoutMaster == null) Log.e(TAG, "MASTER: orderActionButtonsLayout NOT FOUND!");
                if (statusTextViewMaster == null) Log.e(TAG, "MASTER: orderStatusTextView NOT FOUND!");

            } else if ("client".equals(role)) {
                Log.d(TAG, "Initializing views for CLIENT role.");
                statusTextViewClient = itemView.findViewById(R.id.orderStatusTextView_client);
                masterNameTextView = itemView.findViewById(R.id.orderMasterNameTextView_client);
                dateTextViewClient = itemView.findViewById(R.id.orderDateTextView_client);
                problemDescTextViewClient = itemView.findViewById(R.id.orderProblemDescTextView_client);
                addressTextViewClient = itemView.findViewById(R.id.orderAddressTextView_client);
                actionButtonsLayoutClient = itemView.findViewById(R.id.orderActionButtonsLayout_client);


                btnConfirmCompletionClient = itemView.findViewById(R.id.btnConfirmCompletion_client);
                btnOpenDisputeClient = itemView.findViewById(R.id.btnOpenDispute_client);
                btnLeaveReviewClient = itemView.findViewById(R.id.btnLeaveReview_client);
                btnCallMasterClient = itemView.findViewById(R.id.btnCallMaster_client);

                if (actionButtonsLayoutClient == null) Log.e(TAG, "CLIENT: orderActionButtonsLayout_client NOT FOUND!");
                if (statusTextViewClient == null) Log.e(TAG, "CLIENT: orderStatusTextView_client NOT FOUND!");
            }
        }
    }
}