package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

@OpenClassOnDebug
class GroupedProductListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val groupedProductListRepository: GroupedProductListRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: GroupedProductListFragmentArgs by savedState.navArgs()

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    final val productListViewStateData = LiveDataDelegate(savedState, GroupedProductListViewState())
    private var productListViewState by productListViewStateData

    override fun onCleared() {
        super.onCleared()
        groupedProductListRepository.onCleanup()
    }

    init {
        if (_productList.value == null) {
            loadGroupedProducts()
        }
    }

    private fun loadGroupedProducts() {
        val groupedProductIds = navArgs.groupedProductIds.toList()
        val productsInDb = groupedProductListRepository.getGroupedProductList(groupedProductIds)
        if (productsInDb.isNotEmpty()) {
            _productList.value = productsInDb
            productListViewState = productListViewState.copy(isSkeletonShown = false)
        } else {
            productListViewState = productListViewState.copy(isSkeletonShown = true)
        }

        launch { fetchProductReviews(groupedProductIds, loadMore = false) }
    }

    private suspend fun fetchProductReviews(
        groupedProductIds: List<Long>,
        loadMore: Boolean
    ) {
        if (networkStatus.isConnected()) {
            _productList.value = groupedProductListRepository.fetchGroupedProductList(groupedProductIds, loadMore)
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productListViewState = productListViewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false
        )
    }

    @Parcelize
    data class GroupedProductListViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<GroupedProductListViewModel>
}
