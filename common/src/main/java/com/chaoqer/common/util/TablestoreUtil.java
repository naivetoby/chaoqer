package com.chaoqer.common.util;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.core.utils.Pair;
import com.alicloud.openservices.tablestore.core.utils.Preconditions;
import com.alicloud.openservices.tablestore.model.*;

import java.util.ArrayList;
import java.util.List;

public class TablestoreUtil {

    /**
     * 范围查询指定范围内的数据，返回指定页数大小的数据，并能根据offset跳过部分行。
     */
    public static Pair<List<Row>, PrimaryKey> readByPage(SyncClient client, String tableName, List<String> columns, Direction direction, PrimaryKey startKey, PrimaryKey endKey, int offset, int count) {
        Preconditions.checkArgument(offset >= 0, "offset should not be negative.");
        Preconditions.checkArgument(count > 0 && count <= 100, "count should be between 0 and 100.");
        List<Row> rows = new ArrayList<>(count);
        int limit = count;
        int skip = offset;
        PrimaryKey nextStart = startKey;
        // 若查询的数据量很大，则一次请求有可能不会返回所有的数据，需要流式查询所有需要的数据。
        while (limit > 0 && nextStart != null) {
            // 构造GetRange的查询参数。
            // 注意：startPrimaryKey需要设置为上一次读到的位点，从上一次未读完的地方继续往下读，实现流式的范围查询。
            RangeRowQueryCriteria criteria = new RangeRowQueryCriteria(tableName);
            criteria.setInclusiveStartPrimaryKey(nextStart);
            criteria.setExclusiveEndPrimaryKey(endKey);
            if (direction != null) {
                criteria.setDirection(direction);
            } else {
                criteria.setDirection(Direction.FORWARD);
            }
            criteria.setMaxVersions(1);
            // 设置读取某些列
            if (columns != null) {
                columns.forEach(criteria::addColumnsToGet);
            }
            // 需要设置正确的limit，这里期望读出的数据行数最多为完整的一页数据以及需要过滤(offset)的数据
            criteria.setLimit(skip + limit);
            GetRangeRequest request = new GetRangeRequest();
            request.setRangeRowQueryCriteria(criteria);
            GetRangeResponse response = client.getRange(request);
            for (Row row : response.getRows()) {
                if (skip > 0) {
                    skip--; // 对于offset之前的数据，需要过滤掉，采用的策略是读出来后在客户端进行过滤。
                } else {
                    rows.add(row);
                    limit--;
                }
            }
            // 设置下一次查询的起始位点
            nextStart = response.getNextStartPrimaryKey();
        }
        return new Pair<>(rows, nextStart);
    }

}
