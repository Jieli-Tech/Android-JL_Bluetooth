package com.jieli.btsmart.ui.widget.product_dialog;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * 产品设计参数模型
 *
 * @author zqjasonZhong
 * @date 2019/7/23
 */
public class ProductDesignItem implements MultiItemEntity {
    private int type;
    private ProductDesign mFirstProduct;
    private ProductDesign mSecondProduct;

    public final static int VIEW_TYPE_SINGLE = 0;
    public final static int VIEW_TYPE_DOUBLE = 1;
    public final static int VIEW_TYPE_SINGLE_TWO = 2;


    public void setType(int type) {
        this.type = type;
    }

    public ProductDesign getFirstProduct() {
        return mFirstProduct;
    }

    public void setFirstProduct(ProductDesign firstProduct) {
        mFirstProduct = firstProduct;
    }

    public ProductDesign getSecondProduct() {
        return mSecondProduct;
    }

    public void setSecondProduct(ProductDesign secondProduct) {
        mSecondProduct = secondProduct;
    }

    @Override
    public int getItemType() {
        return type;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductDesignItem{" +
                "type=" + type +
                ", mFirstProduct=" + mFirstProduct +
                ", mSecondProduct=" + mSecondProduct +
                '}';
    }
}
