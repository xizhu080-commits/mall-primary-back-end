package com.mall.demo.module.messageRecord.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.demo.module.messageRecord.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {



    // 接收方未读数+1
    @Update("""
UPDATE user_session 
SET unread_count = unread_count + 1 
WHERE user_id = #{userId} AND session_id = #{sessionId}
""")
    void incrementUnreadCount(@Param("userId") String userId, @Param("sessionId") String sessionId);

    // 用户点开对话框时，未读数清零
    @Update("""
UPDATE user_session 
SET unread_count = 0 
WHERE user_id = #{userId} AND session_id = #{sessionId}
""")
    void clearUnreadCount(@Param("userId") String userId, @Param("sessionId") String sessionId);



    /*
     * 根据用户id和sessionId查询对话框
     *
     * */
    @Select(
            """
    SELECT * FROM user_session
    WHERE user_id = #{userId} AND session_id = #{sessionId}
     """
    )
    UserSession selectByUserIdAndSessionId(@Param("userId") String userId, @Param("sessionId") String sessionId);







    /*
     * 获取当前用户所有对话框
     *
     * */

@Select("""
select session_id, user_id,  partner_id, partner_type, partner_name, partner_avatar, unread_count, 
       last_message, last_message_time
from user_session
where user_id = #{currentUserId}
order by last_message_time desc

""")
List<UserSession> getSessionIds(@Param("currentUserId") String currentUserId);



}
