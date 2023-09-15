package org.solovyev.android.calculator.ads;

import static org.solovyev.android.checkout.ProductTypes.IN_APP;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.view.View;

import org.solovyev.android.calculator.AdView;
import org.solovyev.android.calculator.R;
import org.solovyev.android.checkout.CppCheckout;
import org.solovyev.android.checkout.Inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;


public class AdUi {

    @NonNull
    private final CppCheckout checkout;
    @NonNull
    private final Handler handler;
    @Nullable
    AdView adView;
    @Nullable
    private Boolean adFree = null;

    @Inject
    public AdUi(@NonNull CppCheckout checkout, @NonNull Handler handler) {
        this.checkout = checkout;
        this.handler = handler;
    }

    public void onCreate() {
        checkout.start();
    }

    public void onResume() {
        if (adView == null) {
            return;
        }
        adView.resume();
        if (adFree != null) {
            updateAdView();
        } else {
            checkout.loadInventory(Inventory.Request.create().loadAllPurchases(),
                    onMainThread(new Inventory.Callback() {
                        @Override
                        public void onLoaded(@Nonnull Inventory.Products products) {
                            adFree = products.get(IN_APP).isPurchased("ad_free");
                            updateAdView();
                        }
                    }));
        }
    }

    private void updateAdView() {
        if (adFree == null || adView == null) {
            return;
        }

        if (adFree) {
            adView.hide();
        } else {
            adView.show();
        }
    }

    @Nonnull
    private Inventory.Callback onMainThread(@Nonnull final Inventory.Callback callback) {
        return new Inventory.Callback() {
            @Override
            public void onLoaded(@Nonnull final Inventory.Products products) {
                if (handler.getLooper() == Looper.myLooper()) {
                    callback.onLoaded(products);
                    return;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoaded(products);
                    }
                });
            }
        };
    }

    public void onCreateView(@NonNull View view) {
        adView = view.findViewById(R.id.cpp_ad);
    }

    public void onPause() {
        adFree = null;
        if (adView != null) {
            adView.pause();
        }
    }

    public void onDestroyView() {
        if (adView == null) {
            return;
        }
        adView.destroy();
        adView = null;
    }

    public void onDestroy() {
        checkout.stop();
    }
}
