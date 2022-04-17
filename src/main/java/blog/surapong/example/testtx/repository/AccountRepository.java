package blog.surapong.example.testtx.repository;

import blog.surapong.example.testtx.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t From Account t WHERE t.accountId = :accountId")
    Optional<Account> findByIdForUpdate(String accountId);

}