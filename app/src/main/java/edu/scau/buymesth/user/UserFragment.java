package edu.scau.buymesth.user;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;

import base.BaseActivity;
import base.util.GlideCircleTransform;
import base.util.ToastUtil;
import bitmap.BlurTransformation;
import cn.bmob.newim.BmobIM;
import cn.bmob.v3.BmobUser;
import edu.scau.buymesth.R;
import edu.scau.buymesth.adapter.ViewPagerAdapter;
import edu.scau.buymesth.cash.CashMainActivity;
import edu.scau.buymesth.conversation.userlist.UserListFragment;
import edu.scau.buymesth.data.bean.User;
import edu.scau.buymesth.fragment.EmptyActivity;
import edu.scau.buymesth.ui.LoginActivity;
import edu.scau.buymesth.user.address.AddressActivity;
import edu.scau.buymesth.user.mark.MarkActivity;
import edu.scau.buymesth.user.order.OrderFragment;
import edu.scau.buymesth.user.request.RequestFragment;
import edu.scau.buymesth.user.setting.UserSettingActivity;
import edu.scau.buymesth.userinfo.evaluate.EvaluateListActivity;
import edu.scau.buymesth.util.ColorChangeHelper;
import edu.scau.buymesth.util.CompressHelper;
import util.DensityUtil;

/**
 * Created by Jammy on 2016/8/1.
 */
public class UserFragment extends Fragment implements UserContract.View {
    UserPresenter mPresenter;
    ImageView mAvatarIv;
    ImageView mBgUser;
    TextView mNameTv;
    TextView mUserIdTv;
    TextView mLevelTv;
    TextView mLocationTv;
    TextView mSignatureTv;
    View mSettingBtn;
    TextView mScoreTv;
    TextView mPopulationTv;
    RatingBar mRatingBar;
    ViewPager mViewPager;
    TabLayout mTabLayout;
    CoordinatorLayout mCoordinatorLayout;
    LinearLayout userInfoLl;

    ViewGroup mUserInfoLayout;

    private BottomSheetBehavior<NestedScrollView> behavior;
    private SparseArray<Drawable> mLevelDrawableCache = new SparseArray<>();
    private Target<Bitmap> mTartget;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
        mAvatarIv = (ImageView) view.findViewById(R.id.iv_avatar);
        mBgUser = (ImageView) view.findViewById(R.id.bg_user_info);
        mNameTv = (TextView) view.findViewById(R.id.tv_user);
        mUserIdTv = (TextView) view .findViewById(R.id.tv_user_id);
        mLevelTv = (TextView) view.findViewById(R.id.tv_level);
        mLocationTv = (TextView) view.findViewById(R.id.tv_location);
        mSignatureTv = (TextView) view.findViewById(R.id.tv_signature);
        View mAppSettingBtn = (View) view.findViewById(R.id.btn_app_setting);
        mSettingBtn = (View) view.findViewById(R.id.btn_setting);
        mScoreTv = (TextView) view.findViewById(R.id.tv_score);
        mPopulationTv = (TextView) view.findViewById(R.id.tv_population);
        mRatingBar = (RatingBar) view.findViewById(R.id.ratingBar);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mCoordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.cl);
        userInfoLl = (LinearLayout) view.findViewById(R.id.ll_user_info);
        mUserInfoLayout = (ViewGroup) view.findViewById(R.id.rl_user_info);

        mSettingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserSettingActivity.class);
            getActivity().startActivityForResult(intent,303);
        });
        mAppSettingBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity());
            String[] items = {"清除缓存","退出登录"};
            builder.setItems(items, (dialog, which) -> {
                switch (which){
                    case 0:
                        getActivity().runOnUiThread(() -> {
                            AsyncTask task = new CacheCleanTask();
                            task.execute("i");
                        });
                        break;
                    case 1:
                        new AlertDialog.Builder(getActivity()).setTitle("退出登录")
                                .setMessage("确定要退出登录吗？")
                                .setPositiveButton("确定", (dialog1, which1) -> {
                                    BmobUser.logOut();
                                    BmobIM.getInstance().clearAllConversation();
                                    ToastUtil.show("退出登陆");
                                    Intent i = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(i);
                                    getActivity().finish();
                                }).setNegativeButton("取消",null).show();
                        break;
                }
            });
            builder.show();
        });
        view.findViewById(R.id.followed_user_list).setOnClickListener(v -> EmptyActivity.navigate(getActivity(), UserListFragment.class.getName(),null));
        view.findViewById(R.id.address_manage).setOnClickListener(v->{AddressActivity.navigate(getActivity());});
        view.findViewById(R.id.mark_list).setOnClickListener(v->{
            MarkActivity.navigate(getContext());});
        //查看我的评价
        view.findViewById(R.id.my_evaluate).setOnClickListener(v-> EvaluateListActivity.navigate(getContext(),BmobUser.getCurrentUser().getObjectId(),true));
        //钱包
        view.findViewById(R.id.wallet).setOnClickListener(v-> CashMainActivity.navigate(getActivity(),BmobUser.getCurrentUser(User.class)));
        mPopulationTv.setOnClickListener(v->EvaluateListActivity.navigate(getContext(),BmobUser.getCurrentUser().getObjectId(),false));

        UserModel model = new UserModel(getContext());
        mPresenter = new UserPresenter(getContext(),this, model);
        initTab();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.unsubscribe();
    }

    @Override
    public void setUserName(String name) {
        mNameTv.setText(name);
    }

    @Override
    public void setUserId(String id) {
        mUserIdTv.setText(id);
    }

    @Override
    public void setAvatar(String url) {
        Glide.with(getContext()).load(url).crossFade().placeholder(R.mipmap.def_head).transform(new GlideCircleTransform(getContext())).into(mAvatarIv);
        if(url!=null)
        mTartget=Glide.with(this).
                load(url).
                asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).
                transform(new BlurTransformation(getContext(),40)).//高斯模糊处理
                into(mBgUser);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.clear(mTartget);
    }

    @Override
    public void setLevel(Integer exp) {
        Drawable levelBg = mLevelDrawableCache.get(exp / 10 * 10);
        if (levelBg == null) {
            levelBg = ColorChangeHelper.tintDrawable(getContext().getResources().getDrawable(R.drawable.rect_black),
                    ColorStateList.valueOf(ColorChangeHelper.IntToColorValue(exp / 10 * 10)));
            mLevelDrawableCache.put(exp / 10 * 10, levelBg);
        }
        mLevelTv.setBackground(levelBg);
        mLevelTv.setText("LV" + exp / 10);
    }

    @Override
    public void setlocation(String location) {
        mLocationTv.setText(location);
    }

    @Override
    public void setSignature(String signature) {
        mSignatureTv.setText(signature);
    }

    @Override
    public void setScore(String score) {
        mScoreTv.setText(score);
    }

    @Override
    public void setPopulation(String population) {
        mPopulationTv.setText(population);
    }


    @Override
    public void setRatingBar(Float score) {
        if (score != null)
            mRatingBar.setRating(score);
    }

    @Override
    public void initTab() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getActivity().getSupportFragmentManager());
        RequestFragment requestFragment = new RequestFragment();
        OrderFragment orderFragment = new OrderFragment();
        adapter.addTab(requestFragment, "我的请求");
        adapter.addTab(orderFragment,"我的订单");
        mViewPager.setAdapter(adapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTabLayout.setElevation(16);
        }
        mTabLayout.setupWithViewPager(mViewPager);
        NestedScrollView bottomSheet = (NestedScrollView) mCoordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                userInfoLl.setTranslationY(-userInfoLl.getHeight()*slideOffset/3);
                mUserInfoLayout.setTranslationY(userInfoLl.getHeight()*slideOffset/8);
            }
        });
        requestFragment.disallowIntercept(bottomSheet);
        orderFragment.disallowIntercept(bottomSheet);

        userInfoLl.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= 16) {
                            userInfoLl.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                        } else {
                            userInfoLl.getViewTreeObserver()
                                    .removeGlobalOnLayoutListener(this);
                        }
                        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                        behavior.setPeekHeight(wm.getDefaultDisplay().getHeight() - userInfoLl.getHeight() - DensityUtil.dip2px(getContext(), 56f) - ((BaseActivity) getActivity()).getStatusBarHeight());
                    }
                });
    }

    @Override
    public void setEvaluateCount(Integer integer) {
        mPopulationTv.setText(integer+"人评价");
    }

    class CacheCleanTask extends AsyncTask<Object, Object, Object>{
        //后面尖括号内分别是参数（例子里是线程休息时间），进度(publishProgress用到)，返回值 类型

        @Override
        protected Object doInBackground(Object... params) {
            //第二个执行方法,onPreExecute()执行完后执行
            try{
                Glide.get(getContext()).clearDiskCache();
                CompressHelper compressHelper = new CompressHelper(getContext());
                compressHelper.cleanCache();
            }catch (Exception e){
//                ToastUtil.show("清理失败");
                return "清理失败";
            }
//            ToastUtil.show("清理完毕");
            return "清理完毕";
        }

        @Override
        protected void onPostExecute(Object result) {
            //doInBackground返回时触发，换句话说，就是doInBackground执行完后触发
            //这里的result就是上面doInBackground执行后的返回值，所以这里是"执行完毕"
            super.onPostExecute(result);
        }
    }
}
