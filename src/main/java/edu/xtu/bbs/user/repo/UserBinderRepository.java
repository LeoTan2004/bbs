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
     * Find all binding information by user ID
     */
    List<UserBinder> findByUserId(Integer userId);

    /**
     * Find binding information by user ID and bind type
     */
    Optional<UserBinder> findByUserIdAndBindType(Integer userId, BindType bindType);

    /**
     * Find binding information by identifier and bind type
     */
    Optional<UserBinder> findByIdentifierAndBindType(String identifier, BindType bindType);

    /**
     * Check if identifier and bind type combination already exists
     */
    boolean existsByIdentifierAndBindType(String identifier, BindType bindType);

    /**
     * Check if user has already bound this type
     */
    boolean existsByUserIdAndBindType(Integer userId, BindType bindType);

    /**
     * Delete user's specific bind type
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserBinder ub WHERE ub.userId = :userId AND ub.bindType = :bindType")
    int deleteByUserIdAndBindType(@NonNull @Param("userId") Integer userId, @NonNull @Param("bindType") BindType bindType);

    /**
     * Update binding identifier
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserBinder ub SET ub.identifier = :identifier WHERE ub.userId = :userId AND ub.bindType = :bindType")
    int updateIdentifierByUserIdAndBindType(@NonNull @Param("identifier") String identifier,
                                            @NonNull @Param("userId") Integer userId,
                                            @NonNull @Param("bindType") BindType bindType);
}