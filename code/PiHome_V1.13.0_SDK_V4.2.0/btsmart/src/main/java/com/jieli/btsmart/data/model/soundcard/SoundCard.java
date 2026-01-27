package com.jieli.btsmart.data.model.soundcard;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 9:57 AM
 * @desc :
 */
public class SoundCard {


    public boolean hasEq;
    @SerializedName("function")
    public List<Functions> function;

    public static class Title {
        public String zh;
        public String en;

        public String getShowText() {
            return ProductUtil.isChinese() ? zh : en;
        }
    }

    public static class Functions {
        public int id;
        public Title title;
        public String type;
        public String icon_url;
        public int column;
        public boolean paging;
        public int row;
        public List<ListBean> list;

        @Override
        public String toString() {
            return "Functions{" +
                    "id=" + id +
                    ", title=" + title +
                    ", type='" + type + '\'' +
                    ", icon_url='" + icon_url + '\'' +
                    ", colume=" + column +
                    ", paging=" + paging +
                    ", row=" + row +
                    ", list=" + list +
                    '}';
        }

        public static class ListBean {
            public Title title;
            public int index;
            public boolean group;
            public int min;
            public int max;
            public boolean enable;
            public String icon_url;
            public List<ListBean> list;

            @Override
            public String toString() {
                return "ListBean{" +
                        "title=" + title +
                        ", index=" + index +
                        ", group='" + group + '\'' +
                        ", min=" + min +
                        ", max=" + max +
                        ", enable=" + enable +
                        ", list=" + list +
                        '}';
            }
        }

    }


    @Override
    public String toString() {
        return "SoundCard{" +
                "hasEq='" + hasEq + '\'' +
                ", function=" + function +
                '}';
    }
}
