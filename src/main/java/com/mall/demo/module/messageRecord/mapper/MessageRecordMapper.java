package com.mall.demo.module.messageRecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.messageRecord.entity.MessageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageRecordMapper extends BaseMapper<MessageRecord> {


    /*
     * 获取指定对话框聊天记录（支持分页）
     * @param sessionId 会话ID
     * @param beforeId  ‌可选，从这个消息ID之前开始查（查更早的消息）
     * @param limit  ‌每页数量
     */
    @Select("""
    <script>
    select 
        message_record_id,
        message_publisher_id,
        message_publisher_type,
        target_user_id,
        target_user_type,
        content,
        session_id,
        create_time,
        is_read,
        read_time
    from message_record 
    where session_id = #{sessionId}
    <if test="beforeId != null">
        and message_record_id &lt; #{beforeId}
    </if>
    order by create_time desc
    limit #{limit}
    </script>
""")
    List<MessageRecord> getMessageRecordList(
            @Param("sessionId") String sessionId,
            @Param("beforeId") String beforeId,  // 可选
            @Param("limit") int limit
    );




    /*
    * 标记该对话框所有消息为已读
    * */
    @Update( """
    update message_record
    set is_read = true, read_time = now()
    where session_id = #{sessionId}
    and is_read = false
    and target_user_id = #{currentUserId}
    """)
    void markAllAsRead(@Param("sessionId") String sessionId, @Param("currentUserId") String currentUserId);




}
