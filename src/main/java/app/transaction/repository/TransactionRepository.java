package app.transaction.repository;


import app.transaction.model.Transaction;
import app.transaction.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findTop5ByWallet_IdOrderByCreatedOnDesc(UUID walletId);

    void deleteAllByWallet_Id(UUID walletId);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = 'SUCCESSFUL' " +
           "AND t.description LIKE :descriptionPattern " +
           "AND DATE(t.createdOn) = DATE(:date)")
    List<Transaction> findLunchPaymentsFromDate(@Param("type") TransactionType type,
                                                 @Param("descriptionPattern") String descriptionPattern,
                                                 @Param("date") LocalDateTime date);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = 'SUCCESSFUL' " +
           "AND t.description LIKE :descriptionPattern " +
           "AND t.createdOn >= :startDate AND t.createdOn <= :endDate")
    List<Transaction> findLunchPaymentsBetweenDates(@Param("type") TransactionType type,
                                                     @Param("descriptionPattern") String descriptionPattern,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

}
