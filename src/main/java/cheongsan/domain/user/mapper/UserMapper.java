package cheongsan.domain.user.mapper;

import cheongsan.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    User findById(@Param("id") Long id);

    void saveDiagnosis(@Param("id") Long id,
                       @Param("programId") Long programId);

    void updateDailyLimit(@Param("id") Long id,
                          @Param("dailyLimit") BigDecimal dailyLimit,
                          @Param("dailyLimitDate") LocalDateTime dailyLimitDate);

    void save(@RequestBody User user);

    User findByUserId(@Param("userId") String userId);

    User findByEmail(@Param("email") String email);

    User findByUserIdAndEmail(@Param("userId") String userId,
                              @Param("email") String email);

    void updatePassword(@Param("id") Long id,
                        @Param("newPassword") String password);


    void updateProfile(@Param("userId") String userId,
                       @Param("nickname") String nickname,
                       @Param("email") String email);

    void deleteById(@Param("userId") String userId);


    void submitNickname(@Param("userId") String userId,
                        @Param("nickname") String nickname);


    // Connected ID 업데이트
    void updateConnectedId(@Param("userId") Long userId,
                           @Param("connectedId") String connectedId);

    // Connected ID가 있는 모든 사용자 조회
    List<User> findUsersWithConnectedId();

    // Connected ID로 사용자 조회
    User findByConnectedId(@Param("connectedId") String connectedId);

    List<Long> getAllUserIds();

    void deleteDiagnosisByUserId(@Param("userId") Long userId);
}
