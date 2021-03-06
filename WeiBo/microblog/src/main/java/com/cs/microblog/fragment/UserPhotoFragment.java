package com.cs.microblog.fragment;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cs.microblog.R;
import com.cs.microblog.adapter.UserPhotoItemAdapter;
import com.cs.microblog.bean.HomeTimelineList;
import com.cs.microblog.bean.Statuse;
import com.cs.microblog.custom.Constants;
import com.cs.microblog.utils.SharedPreferencesUtils;
import com.cs.microblog.utils.WeiBoUtils;
import com.cs.microblog.view.EndlessRecyclerView;
import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Administrator on 2017/4/27.
 * home fragment,show the user concerned blog
 */

public class UserPhotoFragment extends Fragment {

    @BindView(R.id.erv)
    EndlessRecyclerView erv;

    private String TAG = "UserBlogFragment";

    private ArrayList<Statuse> mStatuses;
    private FragmentActivity mActivity;
    private UserPhotoItemAdapter mPhotoItemAdapter;
    private GridLayoutManager mGridLayoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_like, container, false);
        ButterKnife.bind(this, view);
        erv.disableSwipeRefreshLayout();
        initData();
        initRecyclerView();
        firstFetchData();
        setRefreshListener();
        return view;
    }

    private void initData() {
        mActivity = UserPhotoFragment.this.getActivity();
    }

    private void firstFetchData() {
        WeiBoUtils.getHomeTimelineLists(SharedPreferencesUtils.getString(getContext(),
                Constants.KEY_ACCESS_TOKEN, ""), 0, new WeiBoUtils.CallBack() {
            @Override
            public void onSuccess(Call<HomeTimelineList> call, Response<HomeTimelineList> response) {
                if (response.body() == null) {
                    erv.setDropDownRefreshState(false);
                    return;
                }
                mStatuses.clear();
                mStatuses.addAll(response.body().getStatuses());
                erv.finishDropDownRefreshOnSuccess(mActivity);
            }

            @Override
            public void onFailure(Call<HomeTimelineList> call, Throwable t) {
                Toast.makeText(getContext(), "没有网络了", Toast.LENGTH_SHORT).show();
                erv.finishDropDownRefreshOnFailure(mActivity);
            }
        });
    }

    private void initRecyclerView() {
        mStatuses = new ArrayList<>();
        mPhotoItemAdapter = new UserPhotoItemAdapter(getContext(), erv, mStatuses);
        erv.setAdapter(mPhotoItemAdapter);
        erv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) view.getLayoutParams();
                int spanIndex = layoutParams.getSpanIndex();
                outRect.top = 5;
                if(spanIndex!=0){
                    outRect.left = 5;
                }
            }
        });
        mGridLayoutManager = new GridLayoutManager(getContext(),3);
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemViewType = mPhotoItemAdapter.getItemViewType(position);
                if(itemViewType==mPhotoItemAdapter.ITEM_FOOT){
                    return mGridLayoutManager.getSpanCount();
                }else {
                    return 1;
                }
            }
        });
        erv.setLayoutManager(mGridLayoutManager);
        erv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        Fresco.getImagePipeline().pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        Fresco.getImagePipeline().resume();
                        break;
                }
            }
        });
    }


    /**
     * set refresh listener
     */
    private void setRefreshListener() {
        setAddMoreRefresh();
    }

    /**
     * set add more refresh listener
     */
    private void setAddMoreRefresh() {
        erv.setOnPullUpRefeshListener(new EndlessRecyclerView.OnPullUpRefeshListener() {
            @Override
            public void onPullUpRefesh() {
                WeiBoUtils.getHomeTimelineLists(SharedPreferencesUtils.getString(getContext(),
                        Constants.KEY_ACCESS_TOKEN, ""), mStatuses.get(mStatuses.size() - 1).getId() - 1, new WeiBoUtils.CallBack() {
                    @Override
                    public void onSuccess(Call<HomeTimelineList> call, Response<HomeTimelineList> response) {
                        if (response.body() == null || response.body().getStatuses().size() == 0) {
                            erv.finishPullUpRefreshOnNoMoreData(mActivity);
                        } else {
                            mStatuses.addAll(response.body().getStatuses());
                            erv.finishPullUpRefreshOnSuccess(mActivity);
                        }
                    }

                    @Override
                    public void onFailure(Call<HomeTimelineList> call, Throwable t) {
                        erv.finishPullUpRefreshOnFailure(mActivity);
                    }
                });
            }
        });
    }

    public interface OnRefreshCompleteListener{
        void OnRefreshComplete();
    }

    public void refreshContent(final UserBlogFragment.OnRefreshCompleteListener listener) {
        WeiBoUtils.getHomeTimelineLists(SharedPreferencesUtils.getString(getContext(),
                Constants.KEY_ACCESS_TOKEN, ""), 0, new WeiBoUtils.CallBack() {
            @Override
            public void onSuccess(Call<HomeTimelineList> call, Response<HomeTimelineList> response) {
                if (response.body() == null) {
                    erv.setDropDownRefreshState(false);
                    listener.OnRefreshComplete();
                    return;
                }
                mStatuses.clear();
                mStatuses.addAll(response.body().getStatuses());
                erv.finishDropDownRefreshOnSuccess(mActivity);
                listener.OnRefreshComplete();
            }

            @Override
            public void onFailure(Call<HomeTimelineList> call, Throwable t) {
                Toast.makeText(getContext(), "没有网络了", Toast.LENGTH_SHORT).show();
                erv.finishDropDownRefreshOnFailure(mActivity);
                listener.OnRefreshComplete();
            }
        });
    }
}