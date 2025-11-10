package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.BindType;
import edu.xtu.bbs.user.model.UserBinder;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBinderRepository extends CrudRepository<UserBinder, Integer> {

    /**
     * 根据用户ID查找所有绑定信息
     */
    List<UserBinder> findByUserId(Integer userId);

    /**
     * 根据用户ID和绑定类型查找绑定信息
     */
    Optional<UserBinder> findByUserIdAndBindType(Integer userId, BindType bindType);

    /**
     * 根据标识符和绑定类型查找绑定信息
     */
    Optional<UserBinder> findByIdentifierAndBindType(String identifier, BindType bindType);

    /**
     * 检查标识符和绑定类型是否已存在
     */
    boolean existsByIdentifierAndBindType(String identifier, BindType bindType);

    /**
     * 检查用户是否已绑定该类型
     */
    boolean existsByUserIdAndBindType(Integer userId, BindType bindType);

    /**
     * 删除用户的特定绑定类型
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserBinder ub WHERE ub.userId = :userId AND ub.bindType = :bindType")
    int deleteByUserIdAndBindType(@NonNull @Param("userId") Integer userId, @NonNull @Param("bindType") BindType bindType);

    /**
     * 更新绑定的标识符
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserBinder ub SET ub.identifier = :identifier WHERE ub.userId = :userId AND ub.bindType = :bindType")
    int updateIdentifierByUserIdAndBindType(@NonNull @Param("identifier") String identifier,
                                            @NonNull @Param("userId") Integer userId,
                                            @NonNull @Param("bindType") BindType bindType);
}