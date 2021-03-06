/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.core.trade.failed;

import com.google.inject.Inject;
import io.bisq.common.crypto.KeyRing;
import io.bisq.common.proto.persistable.PersistedDataHost;
import io.bisq.common.proto.persistable.PersistenceProtoResolver;
import io.bisq.common.storage.Storage;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.offer.Offer;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.core.trade.TradableList;
import io.bisq.core.trade.Trade;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

public class FailedTradesManager implements PersistedDataHost {
    private static final Logger log = LoggerFactory.getLogger(FailedTradesManager.class);
    private TradableList<Trade> failedTrades;
    private final KeyRing keyRing;
    private final PriceFeedService priceFeedService;
    private final BtcWalletService btcWalletService;
    private final Storage<TradableList<Trade>> tradableListStorage;

    @Inject
    public FailedTradesManager(KeyRing keyRing, PriceFeedService priceFeedService,
                               PersistenceProtoResolver persistenceProtoResolver,
                               BtcWalletService btcWalletService,
                               @Named(Storage.STORAGE_DIR) File storageDir) {
        this.keyRing = keyRing;
        this.priceFeedService = priceFeedService;
        this.btcWalletService = btcWalletService;
        tradableListStorage = new Storage<>(storageDir, persistenceProtoResolver);

    }

    @Override
    public void readPersisted() {
        this.failedTrades = new TradableList<>(tradableListStorage, "FailedTrades");
        failedTrades.forEach(e -> e.getOffer().setPriceFeedService(priceFeedService));
        failedTrades.forEach(trade -> {
            trade.getOffer().setPriceFeedService(priceFeedService);
            trade.setTransientFields(tradableListStorage, btcWalletService);
        });
    }

    public void add(Trade trade) {
        if (!failedTrades.contains(trade))
            failedTrades.add(trade);
    }

    public boolean wasMyOffer(Offer offer) {
        return offer.isMyOffer(keyRing);
    }

    public ObservableList<Trade> getFailedTrades() {
        return failedTrades.getList();
    }

    public Optional<Trade> getTradeById(String id) {
        return failedTrades.stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    public Stream<Trade> getLockedTradesStream() {
        return failedTrades.stream()
                .filter(Trade::isFundsLockedIn);
    }
}
