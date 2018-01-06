package com.example.administrator.slidingmenuview;

        import android.animation.Animator;
        import android.animation.ObjectAnimator;
        import android.content.Context;
        import android.content.IntentFilter;
        import android.graphics.Rect;
        import android.support.v4.view.GestureDetectorCompat;
        import android.support.v4.view.MotionEventCompat;
        import android.support.v7.widget.RecyclerView;
        import android.util.Log;
        import android.view.GestureDetector;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewConfiguration;
/**
 *
 * @author New
 * @date 2018/1/3
 */

public class ItemSlideHelper implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {

    private static final String TAG = "ItemSwipeHelper";

    //默认的菜单的展开长度
    private final int DEFAULT_DURATION = 200;

    private View mTargetView;

    private int mActivePointerId;

    //滑动距离的常量
    private int mTouchSlop;
    private int mMaxVelocity; //快速滑动
    private int mMinVelocity;
    private int mLastX;
    private int mLastY;
    //拖拽
    private boolean mIsDragging;
    //动画
    private Animator mExpandAndCollapseAnim;
    //系统的滑动手势类
    private GestureDetectorCompat mGestureDetector;

    //回调接口，查找用户点击的item
    private Callback mCallback;

    public ItemSlideHelper(Context context, Callback callback) {
        this.mCallback = callback;

        //手势监听，用于处理fling（抛，稍微滑动后立即松开）
        mGestureDetector = new GestureDetectorCompat(context, this);
        //返回指定的Context配置，配置的值取决于context的各种参数：显示的尺寸或密度
        ViewConfiguration configuration = ViewConfiguration.get(context);
        //获取滑动的像素距离
        mTouchSlop = configuration.getScaledTouchSlop();
        Log.d(TAG, "ItemSlideHelper: mTouchSlop = " + mTouchSlop);
        //（抛）滑动速度
        mMaxVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinVelocity = configuration.getScaledMinimumFlingVelocity();
        Log.d(TAG, "ItemSlideHelper: mMaxVelocity = " + mMaxVelocity + "   mMinVelocity" + mMinVelocity);


    }

    /**
     * 拦截触碰事件
     * 默认返回false，返回true表示拦截。
     */

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        Log.d(TAG, "onInterceptTouchEvent: " + PrintLogMessage(e.getAction(), "ITE"));

        //当前的触摸事件
        int action = e.getAction();
        //获取用户手指点击的xy坐标
        int x = (int) e.getX();
        int y = (int) e.getY();
        Log.d(TAG, "onInterceptTouchEvent:  recyclerviewScrollState = " + rv.getScrollState() +PrintLogMessage(rv.getScrollState(), "RSS"));

        Log.d(TAG, "onInterceptTouchEvent: TargetView  " + mTargetView);
        //如果RecyclerView滚动状态不是空闲idle， targetView不为空
        if (rv.getScrollState() != RecyclerView.SCROLL_STATE_IDLE){
            if (mTargetView != null){
                //隐藏已打开
                smoothHorizontalExpandOrCollapse(DEFAULT_DURATION / 2);
                mTargetView = null;
            }
            return false;
        }

        //如果正在运行动画，直接拦截什么都不做
        if (mExpandAndCollapseAnim != null && mExpandAndCollapseAnim.isRunning()){
            return true;
        }
        //是否需要拦截
        boolean needIntercept = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //获取指针ID
                mActivePointerId = MotionEventCompat.getPointerId(e, 0);
                mLastX = (int) e.getX();
                mLastY = (int) e.getY();

                /*
                 * 如果之前有一个已经打开的项目，当此次点击事件没有发生在右侧的菜单
                 * 则返回true，
                 * 如果点击的是右侧菜单那么返回False，
                 * 这样做是因为菜单需要响应OnClick
                 */
                if (mTargetView != null) {
                    return !inView(x, y);
                }

                //查找需要显示菜单的view
                mTargetView = mCallback.findTargetView(x, y);

                Log.d(TAG, "onInterceptTouchEvent: mLx=" + mLastX + "  mLY=" + mLastY + "  X =" + x + "  Y =" + y);
                break;
            case MotionEvent.ACTION_MOVE:
                //需要偏移的距离
                int deltaX = (x - mLastX);
                int deltaY = (y - mLastY);
                Log.d(TAG, "onInterceptTouchEvent: MOVE    mLx=" + mLastX + "  mLY=" + mLastY + "  X =" + x + "  Y =" + y);
                Log.d(TAG, "onInterceptTouchEvent: delta x = " + deltaX + "  deltaY = " +deltaY);

                if (Math.abs(deltaY) > Math.abs(deltaX)){
                    return false;
                }

                //如果移动距离达到要求，则拦截
                needIntercept = mIsDragging = mTargetView != null
                        && Math.abs(deltaX) >= mTouchSlop;

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /*
                * 这里没有发生过拦截事件
                **/
                if (isExpanded()){
                    //抬起的位置，是否在item中
                    if (inView(x, y)){
                        //如果走这那行这个ACTION_UP的事件会发生在右侧的菜单中
                        Log.d(TAG, "click item");
                    } else {
                        //拦截事件，防止targetView执行onClick事件
                        needIntercept = true;
                    }

                    //折叠菜单
                    smoothHorizontalExpandOrCollapse(DEFAULT_DURATION / 2);
                }

                mTargetView = null;

                break;
            default:
                break;
        }

        return needIntercept;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        Log.d(TAG, "onTouchEvent: " + e.getAction());

        if (mExpandAndCollapseAnim != null && mExpandAndCollapseAnim.isRunning() || mTargetView == null){
            return;
        }

        //如果要响应fling事件，设置将mIsDragging设为false
        //手势监听类使用了事件
        if (mGestureDetector.onTouchEvent(e)){
            mIsDragging = false;
            return;
        }

         int x = (int) e.getX();
         int y = (int) e.getY();
         /*
         getActionMasked() : 触摸的动作,按下，抬起，滑动，多点按下，多点抬起
          */
         int action = e.getAction();
         switch (action){
             case MotionEvent.ACTION_DOWN:
                    //RecyclerView不会转发这个Down事件

                 break;
           case MotionEvent.ACTION_MOVE:
                 int deltaX = (int) (mLastX - e.getX());
                 Log.d(TAG, "onTouchEvent: mLastx " + mLastX + "  -  e.getX() " + e.getX());

                 if (mIsDragging) {
                     horizontalDrag(deltaX);
                 }
                 mLastX = x;
                 break;
             case MotionEvent.ACTION_UP:
                 if (mIsDragging) {
                     if (!smoothHorizontalExpandOrCollapse(0) && isCollapsed()) {
                         mTargetView = null;
                     }
                         mIsDragging = false;
                 }
                 break;
         }

    }


    /**
     * 根据touch事件来滚动View的scrollX
     *
     * @param delta
     */
    private void horizontalDrag(int delta) {
        int scrollX = mTargetView.getScrollX();
        int scrollY = mTargetView.getScrollY();
        Log.d(TAG, "horizontalDrag: scrollY = " + scrollY);
        if ((scrollX + delta) <= 0) {
            mTargetView.scrollTo(0,scrollY);
            return;
        }

        int horRange = getHorizontalRange();
        scrollX += delta;
        if (Math.abs(scrollX) < horRange) {
            mTargetView.scrollTo(scrollX, scrollY);
        } else {
            mTargetView.scrollTo(horRange, scrollY);
        }
    }

    /**
     *根据targetView的scrollX计算出targetView的偏移，
     * 这样就知道这个point是否在有则的菜单中
     * @param x
     * @param y
     * @return
     */
    private boolean inView(int x, int y){
        if (mTargetView == null){
            return false;
        }
        int scrollX = mTargetView.getScrollX();
        int left = mTargetView.getWidth() - scrollX;
        int top = mTargetView.getTop();
        int right = left + getHorizontalRange();
        int bottom = mTargetView.getBottom();
        Rect rect = new Rect(left, top, right, bottom);
        //判断x y 的点是否在这个矩形内
        return rect.contains(x, y);
    }

    /**
     * 是否已展开，用当前的距离去对比要展开的距离
     * @return
     */
    private boolean isExpanded() {
        return mTargetView != null && mTargetView.getScaleX() == getHorizontalRange();
    }

    /**
     * 是否已折叠,初始状态
     * @return
     */
    private boolean isCollapsed() {
        return mTargetView != null && mTargetView.getScrollX() == 0;
    }

    /**
     * viewholder 获取targetView的水平范围
     * @return
     */
    public int getHorizontalRange() {
        RecyclerView.ViewHolder viewHolder = mCallback.getChildViewHoder(mTargetView);
        return mCallback.getHorizontalRange(viewHolder);
    }


    /**
     * 根据当前scrollX的位置判断是展开还是折叠
     *
     * @param velocityX
     * 如果不等于0那么这是一次fling事件，
     * 否则是一次ACTION_UP 或者 ACTION_CANCEL
     * @return
     */
    private boolean smoothHorizontalExpandOrCollapse(float velocityX) {
        //获取滚动的x轴的距离大小
        int scrollX = mTargetView.getScrollX();
        //水平范围
        int scrollRange = getHorizontalRange();

        //展开和折叠动画
        if (mExpandAndCollapseAnim != null) {
            return false;
        }
        //随着时间推移变化的值
        int to = 0;
        //默认长短
        int duration = DEFAULT_DURATION;

        //根据不同的水平方向滑动距离的判断  x：左负 右正  y: 上负 下正
        if (velocityX == 0) {
            //如果已经展开一半，平滑展开
            if (scrollX > scrollRange / 2){
                to = scrollRange;
            }
        } else {
            if (velocityX > 0){
                to = 0;
            } else {
                to = scrollRange;
            }
            duration = (int) (1.f - Math.abs(velocityX / mMaxVelocity) * DEFAULT_DURATION);

            Log.d(TAG, "smoothHorizontalExpandOrCollapse: Duration:" + duration);
        }
        if (to == scrollX) {
            return false;
        }
        //[int] to: A set of values that the animation will animate between over time.
        mExpandAndCollapseAnim = ObjectAnimator.ofInt(mTargetView, "scrollX", to);
        mExpandAndCollapseAnim.setDuration(duration);
        mExpandAndCollapseAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mExpandAndCollapseAnim = null;
                if (isCollapsed()){
                    mTargetView = null;

                    Log.d(TAG, "onAnimationEnd: ");
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //
                mExpandAndCollapseAnim = null;

                Log.d(TAG, "onAnimationCancel: ");

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        //启动动画
         mExpandAndCollapseAnim.start();

        return true;

    }

    /**
     * 回调接口
     * 用于查找点击位置的item的具体数据
     */
    public interface Callback {
        //获取随即高度
        int getHorizontalRange(RecyclerView.ViewHolder holder);
        //获取子项的ViewHolder
        RecyclerView.ViewHolder getChildViewHoder(View childView);
        //查询目标视图View
        View findTargetView(float x, float y);

    }

    public String PrintLogMessage(int id, String fromPosition){
        if (fromPosition == "RSS"){
            switch (id){
                case 1:
                    return "移动";
                case 0:
                    return "空闲";
            }
        } else if (fromPosition == "ITE") {
            switch (id) {
                case 0:
                    return "Down ";
                case 1:
                    return "抬起UP";
                case 2:
                    return "移动";
                case 3:
                    return "取消Cancel";
            }
        }
        return "";

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

}

