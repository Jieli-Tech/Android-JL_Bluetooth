package com.jieli.btsmart.demo;

import com.jieli.filebrowse.FileBrowseManager;

/**
 * FileSettingDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件操作配置示例代码
 * @since 2024/12/9
 */
class FileSettingDemo {

    void setFileBrowsePage() {
        int pageSize = 30; //每页请求条目数量， 建议每页数量不超过30
        //需要在文件浏览前设置才会生效
        FileBrowseManager.getInstance().setPageSize(pageSize);
    }
}
