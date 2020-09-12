package com.woocommerce.android.ui.orders.details

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.orders.details.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.details.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.details.OrderNavigationTarget.ViewRefundedProducts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderNavigationTarget) {
        when (target) {
            is ViewOrderStatusSelector -> {
                val action = OrderDetailFragmentNewDirections
                    .actionOrderDetailFragmentToOrderStatusSelectorDialog(
                        target.currentStatus, target.orderStatusList
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is IssueOrderRefund -> {
                val action = OrderDetailFragmentNewDirections
                    .actionOrderDetailFragmentToIssueRefund(target.remoteOrderId)
                fragment.findNavController().navigateSafely(action)
            }
            is ViewRefundedProducts -> {
                val action = OrderDetailFragmentNewDirections
                    .actionOrderDetailFragmentToRefundDetailFragment(target.remoteOrderId)
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
