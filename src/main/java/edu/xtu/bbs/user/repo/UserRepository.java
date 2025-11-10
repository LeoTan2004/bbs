package edu.xtu.bbs.user.repo;

import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    @Transactional
    @Query("update User u set u.passwordHash = :passwordHash where u.id = :id")
    @Modifying
    int updatePasswordById(@NonNull @Param("passwordHash") String passwordHash, @NonNull @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update User u set u.nickname = :nickname, u.bio = :bio, u.profileSlug = :profileSlug where u.id = :id")
    int updateProfileById(@Nullable @Param("nickname") String nickname,
                          @Nullable @Param("bio") String bio,
                          @Nullable @Param("profileSlug") String profileSlug,
                          @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update User u set u.verifiedId = :verifiedId where u.id = :id")
    int updateVerifiedIdById(@Nullable @Param("verifiedId") Integer verifiedId, @NonNull @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update User u set u.role = :role where u.id = :id")
    int updateRoleById(@NonNull @Param("role") Role role, @Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("update User u set u.status = :status where u.id = :id")
    int updateStatusById(@NonNull @Param("status") Status status, @Param("id") Integer id);

    boolean existsByUsername(String username);

}