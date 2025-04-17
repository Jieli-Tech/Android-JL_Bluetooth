package com.jieli.btsmart.ui.widget.product_dialog;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.ProductUtil;

import java.util.List;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * 产品展示适配器
 *
 * @author zqjasonZhong
 * @date 2019/7/23
 */
public class ProductAdapter extends BaseMultiItemQuickAdapter<ProductDesignItem, BaseViewHolder> {

    public ProductAdapter(List<ProductDesignItem> data) {
        super(data);
        addItemType(ProductDesignItem.VIEW_TYPE_SINGLE, R.layout.item_product_single1);
        addItemType(ProductDesignItem.VIEW_TYPE_DOUBLE, R.layout.item_product_double1);
        addItemType(ProductDesignItem.VIEW_TYPE_SINGLE_TWO, R.layout.item_product_single_two);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, ProductDesignItem item) {
        if (item == null) return;
        int itemViewType = getItemViewType(getItemPosition(item));
        switch (itemViewType) {
            case ProductDesignItem.VIEW_TYPE_SINGLE:
                ProductDesign productDesign = item.getFirstProduct();
                if (productDesign != null) {
                    ImageView designImg = helper.getView(R.id.iv_product_1);
                    TextView quantityTv = helper.getView(R.id.item_product_quantity);
                    updateProductDesign(designImg, productDesign.isGif(), productDesign.getImageUrl(), productDesign.getFailedRes());
                    if (productDesign.getAction() == ProductDesign.ACTION_SHOW_QUANTITY) {
                        updateQuantity(quantityTv, productDesign.isCharging(), (int) productDesign.getQuantity());
                    } else {
                        updateQuantity(quantityTv, false, 0);
                    }
                }
                break;
            case ProductDesignItem.VIEW_TYPE_DOUBLE:
                ProductDesign firstProduct = item.getFirstProduct();
                if (firstProduct != null) {
                    ImageView designImg = helper.getView(R.id.item_product_left_design);
                    TextView quantityTv = helper.getView(R.id.item_product_left_quantity);
                    updateProductDesign(designImg, firstProduct.isGif(), firstProduct.getImageUrl(), firstProduct.getFailedRes());
                    if (firstProduct.getAction() == ProductDesign.ACTION_SHOW_QUANTITY) {
                        updateQuantity(quantityTv, firstProduct.isCharging(), (int) firstProduct.getQuantity());
                    } else {
                        updateQuantity(quantityTv, false, 0);
                    }
                }
                ProductDesign secondProduct = item.getSecondProduct();
                if (secondProduct != null) {
                    ImageView designImg = helper.getView(R.id.item_product_right_design);
                    TextView quantityTv = helper.getView(R.id.item_product_right_quantity);
                    updateProductDesign(designImg, secondProduct.isGif(), secondProduct.getImageUrl(), secondProduct.getFailedRes());
                    if (secondProduct.getAction() == ProductDesign.ACTION_SHOW_QUANTITY) {
                        updateQuantity(quantityTv, secondProduct.isCharging(), (int) secondProduct.getQuantity());
                    } else {
                        updateQuantity(quantityTv, false, 0);
                    }
                }
                break;
            case ProductDesignItem.VIEW_TYPE_SINGLE_TWO:
                ProductDesign design = item.getFirstProduct();
                if (design != null) {
                    ImageView ivLeft = helper.getView(R.id.iv_product_single_left);
                    ImageView ivRight = helper.getView(R.id.iv_product_single_right);
                    TextView tvQuantity = helper.getView(R.id.tv_product_single_quantity);
                    updateProductDesign(ivLeft, ProductUtil.isGifFile(design.getImageUrl()), design.getImageUrl(), design.getFailedRes());
                    updateProductDesign(ivRight, ProductUtil.isGifFile(design.getFileUrl()), design.getFileUrl(), design.getFailedFileRes());
                    if (design.getAction() == ProductDesign.ACTION_SHOW_QUANTITY) {
                        updateQuantity(tvQuantity, design.isCharging(), (int) design.getQuantity());
                    } else {
                        updateQuantity(tvQuantity, false, 0);
                    }
                }
                break;
        }
    }

    private void updateProductDesign(ImageView imageView, boolean isGif, String url, int failResId) {
        if (imageView != null) {
            if (failResId <= 0) {
                failResId = R.drawable.ic_default_product_design;
            }
            if (url == null) {
                imageView.setImageResource(failResId);
                return;
            }
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .override(SIZE_ORIGINAL)
                    .fallback(failResId);
            if (isGif) {
                Glide.with(getContext())
                        .asGif()
                        .apply(options)
                        .load(url)
                        .error(failResId)
                        .into(imageView);
            } else {
                Glide.with(getContext())
                        .asBitmap()
                        .apply(options)
                        .load(url)
                        .error(failResId)
                        .into(imageView);
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateQuantity(TextView mQuantityTv, boolean isCharging, int quantity) {
        if (mQuantityTv != null) {
            if (quantity > 0) {
                if (quantity > 100) {
                    quantity = 100;
                }
                mQuantityTv.setVisibility(View.VISIBLE);
                String text = quantity + "%";
                mQuantityTv.setText(text);
                int resId;
                if (isCharging) {
                    resId = R.drawable.ic_charging;
                } else {
                    if (quantity <= 20) {
                        resId = R.drawable.ic_quantity_0;
                    } else if (quantity <= 35) {
                        resId = R.drawable.ic_quantity_25;
                    } else if (quantity <= 50) {
                        resId = R.drawable.ic_quantity_50;
                    } else if (quantity <= 75) {
                        resId = R.drawable.ic_quantity_75;
                    } else {
                        resId = R.drawable.ic_quantity_100;
                    }
                }
                mQuantityTv.setCompoundDrawablesWithIntrinsicBounds(null, getContext().getResources().getDrawable(resId), null, null);
            } else {
                mQuantityTv.setVisibility(View.INVISIBLE);
            }
        }
    }
}
