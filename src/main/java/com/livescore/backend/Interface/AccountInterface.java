package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountInterface extends JpaRepository<Account,Long> {

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Account> findById(@Param("id") Long id);

    @Query("SELECT (COUNT(a) > 0) FROM Account a WHERE a.id = :id AND a.isDeleted = false")
    boolean existsById(@Param("id") Long id);

    @Query("SELECT a FROM Account a WHERE a.isDeleted = false")
    List<Account> findAll();

    boolean existsByUsername(String username);

    @Query("SELECT (COUNT(a) > 0) FROM Account a WHERE a.username = :username AND a.isDeleted = false")
    boolean existsActiveByUsername(@Param("username") String username);

    @Query("SELECT a FROM Account a WHERE a.username = :username AND a.isDeleted = false")
    Account findByUsername(@Param("username") String username);

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Account> findActiveById(@Param("id") Long id);
    
    @Query("SELECT a FROM Account a WHERE a.username = :id AND a.isDeleted = false")
    Optional<Account> findActiveByUsername(@Param("id") String id);

    @Query("SELECT a FROM Account a WHERE a.isDeleted = false")
    List<Account> findAllActive();

    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdIncludingDeleted(@Param("id") Long id);

}
