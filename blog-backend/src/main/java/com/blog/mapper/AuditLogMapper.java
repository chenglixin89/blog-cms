package com.blog.mapper;

import com.blog.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuditLogMapper {

    @Insert("""
        INSERT INTO blog_audit_log (operator, module, action, target_type, target_id, detail, ip, user_agent)
        VALUES (#{operator}, #{module}, #{action}, #{targetType}, #{targetId}, #{detail}, #{ip}, #{userAgent})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog log);

    @Select("""
        <script>
        SELECT id, operator, module, action, target_type, target_id, detail, ip, user_agent, created_at
        FROM blog_audit_log
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (operator LIKE CONCAT('%', #{keyword}, '%')
            OR detail LIKE CONCAT('%', #{keyword}, '%')
            OR target_type LIKE CONCAT('%', #{keyword}, '%')
            OR ip LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="module != null and module != ''">
          AND module = #{module}
        </if>
        <if test="action != null and action != ''">
          AND action = #{action}
        </if>
        ORDER BY created_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<AuditLog> selectPage(
        @Param("keyword") String keyword,
        @Param("module") String module,
        @Param("action") String action,
        @Param("offset") int offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM blog_audit_log
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (operator LIKE CONCAT('%', #{keyword}, '%')
            OR detail LIKE CONCAT('%', #{keyword}, '%')
            OR target_type LIKE CONCAT('%', #{keyword}, '%')
            OR ip LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="module != null and module != ''">
          AND module = #{module}
        </if>
        <if test="action != null and action != ''">
          AND action = #{action}
        </if>
        </script>
        """)
    long countPage(
        @Param("keyword") String keyword,
        @Param("module") String module,
        @Param("action") String action
    );
}
