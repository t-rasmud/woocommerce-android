package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.fragment_grouped_product_list.*
import javax.inject.Inject

class GroupedProductListFragment : BaseFragment(), OnLoadMoreListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: GroupedProductListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()
    private lateinit var productListAdapter: GroupedProductListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grouped_product_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productListAdapter = GroupedProductListAdapter(activity, this)
        productsRecycler.layoutManager = LinearLayoutManager(activity)
        productsRecycler.adapter = productListAdapter

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        productsRecycler.isMotionEventSplittingEnabled = false
    }

    private fun setupObservers() {
        viewModel.productListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        })

        viewModel.productList.observe(viewLifecycleOwner, Observer {
            productListAdapter.setProductList(it)
        })
    }

    private fun showLoadMoreProgress(show: Boolean) {
        loadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> {
                skeletonView.show(productsRecycler, R.layout.skeleton_product_list, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }

    override fun onRequestLoadMore() {
        // TODO
    }
}
