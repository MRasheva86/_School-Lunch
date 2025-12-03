package app.wallet.repository;

import app.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

   Wallet findByOwnerId(UUID uuid);

}
