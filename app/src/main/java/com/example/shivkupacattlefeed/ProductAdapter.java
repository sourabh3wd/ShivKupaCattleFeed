package com.example.shivkupacattlefeed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onSellClick(Product product);
        void onRestockClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvBrand.setText(product.brand);
        holder.tvCategory.setText(product.category);
        holder.tvVariant.setText(product.variant);
        holder.tvPrice.setText("किंमत: ₹" + product.price + " / " + product.unit);

        try {
            double qty = Double.parseDouble(product.quantity);
            if (qty <= 0) {
                holder.tvStock.setTextColor(android.graphics.Color.RED);
                holder.tvStock.setText("माल संपला आहे! (Out of Stock)");
                holder.tvLowStockBadge.setVisibility(View.VISIBLE);
                holder.tvLowStockBadge.setText("साठा संपला!");
            } else if (qty <= 5) {
                holder.tvStock.setTextColor(android.graphics.Color.RED);
                holder.tvStock.setText("कमी साठा! शिल्लक: " + product.quantity + " " + product.unit);
                holder.tvLowStockBadge.setVisibility(View.VISIBLE);
                holder.tvLowStockBadge.setText("साठा कमी!");
            } else {
                holder.tvStock.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
                holder.tvStock.setText("शिल्लक: " + product.quantity + " " + product.unit);
                holder.tvLowStockBadge.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            holder.tvStock.setText("शिल्लक: " + product.quantity + " " + product.unit);
            holder.tvLowStockBadge.setVisibility(View.GONE);
        }

        holder.btnSell.setOnClickListener(v -> listener.onSellClick(product));
        holder.btnRestock.setOnClickListener(v -> listener.onRestockClick(product));
    }

    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrand, tvCategory, tvVariant, tvPrice, tvStock, tvLowStockBadge;
        Button btnSell, btnRestock;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrand = itemView.findViewById(R.id.tvProductBrand);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvVariant = itemView.findViewById(R.id.tvProductVariant);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvProductStock);
            tvLowStockBadge = itemView.findViewById(R.id.tvLowStockBadge);
            btnSell = itemView.findViewById(R.id.btnSell);
            btnRestock = itemView.findViewById(R.id.btnRestock);
        }
    }
}
