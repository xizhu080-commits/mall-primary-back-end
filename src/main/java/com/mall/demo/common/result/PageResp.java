package com.mall.demo.common.result;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果响应封装类
 */
@Data

public class PageResp<T> implements Serializable {

    /**
     * 总记录数（用于前端展示“共 1000 条”）
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 实际的数据列表（如：具体的商品列表、订单列表）
     */
    private List<T> list;

    /**
     * 快捷构造方法
     */
    public static <T> PageResp<T> of(Long total, Long pageSize, Long pageNum, List<T> list) {
        PageResp<T> resp = new PageResp<>();
        resp.setTotal(total);
        resp.setPageSize(pageSize);
        resp.setPageNum(pageNum);

        long pages = 0;
        if (pageSize != null && pageSize > 0) {
            pages = total % pageSize == 0
                    ? total / pageSize
                    : total / pageSize + 1;
        }
        resp.setPages(pages);
        resp.setList(list);
        return resp;
    }

}