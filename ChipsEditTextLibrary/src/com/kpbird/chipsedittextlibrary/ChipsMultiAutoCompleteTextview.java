package com.kpbird.chipsedittextlibrary;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

public class ChipsMultiAutoCompleteTextview extends MultiAutoCompleteTextView {

	public interface OnChipClickListener {
		public void onChipClick(ChipsMultiAutoCompleteTextview v, int start, int end);
	}

	private final String TAG = "ChipsMultiAutoCompleteTextview";
	private int chipsBackground;
	private int chipsDrawableLeft, chipsDrawableTop, chipsDrawableRight, chipsDrawableBottom;
	private int chipsTextColor;
	private OnChipClickListener onChipClickListener;
	private OnTouchListener onTouchListener;
	private OnItemClickListener onItemClickListener;
	private boolean ignoreNotification;
	
	/* Constructor */
	public ChipsMultiAutoCompleteTextview(Context context) {
		super(context);
		init(context);
	}
	/* Constructor */
	public ChipsMultiAutoCompleteTextview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	/* Constructor */
	public ChipsMultiAutoCompleteTextview(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	/* set listeners for item click and text change */
	private void init(Context context){
		chipsBackground = R.drawable.chips_edittext_gb;
		chipsDrawableLeft = R.drawable.android;
		chipsTextColor = Color.BLACK;
		super.setOnTouchListener(new ClickableSpansSupervisor());
		setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		super.setOnItemClickListener(oicl);
		addTextChangedListener(tw);
	}
	private OnItemClickListener oicl = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			ChipsItem ci = (ChipsItem) getAdapter().getItem(position);
			
			generateChips(); // call generate chips when user select any item from auto complete
			
			if(onItemClickListener != null) {
				onItemClickListener.onItemClick(parent, view, position, id);
			}
		}
	};
	/*TextWatcher, If user type any country name and press comma then following code will regenerate chips */
	private TextWatcher tw = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if(!ignoreNotification){
				if(TextUtils.indexOf(s.subSequence(start, start + count), ',') > -1)
					generateChips(); // generate chips
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
		@Override
		public void afterTextChanged(Editable s) {}
	};
	/*This function has whole logic for chips generate*/
	public void generateChips(){
		if(getText().toString().contains(",")) // check comman in string
		{
			
			SpannableStringBuilder ssb = new SpannableStringBuilder(getText());
			ssb.clearSpans();
			// split string wich comma
			String chips[] = getText().toString().trim().split(",");
			int x = 0;
			// loop will generate ImageSpan for every country name separated by comma
			for(String c : chips){
				// inflate chips_edittext layout 
				LayoutInflater lf = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
				TextView textView = (TextView) lf.inflate(R.layout.chips_edittext, null);
				textView.setBackgroundResource(chipsBackground);
				ChipsAdapter adapter = (ChipsAdapter) getAdapter();
				int leftImage = chipsDrawableLeft;
				if(adapter != null) {
					leftImage = adapter.getImage(c);
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(leftImage,
						chipsDrawableTop, chipsDrawableRight, chipsDrawableBottom);
				textView.setTextColor(chipsTextColor);
				textView.setText(c); // set text
				// set max height
				int height = getHeight();
				if(height == 0) {
					int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
					measure(spec, spec);
					height = getMeasuredHeight();
				}
				textView.setMaxHeight(height - getPaddingTop() - getPaddingBottom());

				// capture bitmapt of genreated textview
				int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
				textView.measure(spec, spec);
				textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
				Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(),Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(b);
				canvas.translate(-textView.getScrollX(), -textView.getScrollY());
				textView.draw(canvas);
				textView.setDrawingCacheEnabled(true);
				Bitmap cacheBmp = textView.getDrawingCache();
				Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
				textView.destroyDrawingCache();  // destory drawable

				// create bitmap drawable for imagespan
				BitmapDrawable bmpDrawable = new BitmapDrawable(getResources(), viewBmp);
				bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(), bmpDrawable.getIntrinsicHeight());

				// create and set imagespan 
				ssb.setSpan(new ImageSpan(bmpDrawable), x, x + c.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				// create and set clickablespan
				ssb.setSpan(new ChipsSpan(), x, x + c.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				x = x + c.length() + 1;
			}
			// set chips span 
			ignoreNotification = true;
			setTextKeepState(ssb);
			ignoreNotification = false;
		}
		
		
	}
	
	
	void dispatchChipClick(ChipsSpan span) {
		if(onChipClickListener != null) {
			Spannable spannable = getText();
			onChipClickListener.onChipClick(this, spannable.getSpanStart(span), spannable.getSpanEnd(span));
		}
	}
	
	
	public void setChipsBackgroundResource(int resid) {
		chipsBackground = resid;
	}
	
	
	public void setChipsCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
		chipsDrawableLeft = left;
		chipsDrawableTop = top;
		chipsDrawableRight = right;
		chipsDrawableBottom = bottom;
	}
	
	
	public void setChipsTextColor(int color) {
		chipsTextColor = color;
	}
	
	
	public void setOnChipClickListener(OnChipClickListener l) {
		onChipClickListener = l;
	}
	
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {
		onTouchListener = l;
	}
	
	
	@Override
	public void setOnItemClickListener(OnItemClickListener l) {
		onItemClickListener = l;
	}
	
	
	static class ChipsSpan extends ClickableSpan {

		@Override
		public void onClick(View widget) {
			ChipsMultiAutoCompleteTextview chipView = (ChipsMultiAutoCompleteTextview) widget;
			chipView.dispatchChipClick(this);
		}

	}
	

	private class ClickableSpansSupervisor implements OnTouchListener {
		private boolean mIsPressed;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			TextView textView = (TextView) v;

			int action = event.getAction();
			CharSequence text = textView.getText();
			if (!(text instanceof Spannable)) return false;
			Spanned buffer = (Spanned) textView.getText();

			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= textView.getTotalPaddingLeft();
				y -= textView.getTotalPaddingTop();

				x += textView.getScrollX();
				y += textView.getScrollY();

				Layout layout = textView.getLayout();
				if(layout == null) return dispatchTouch(false, v, event);
				ArrayList<ClickableSpan> linkList = new ArrayList<ClickableSpan>();

				int line = layout.getLineForVertical(y);
				if(x < layout.getLineWidth(line)) {
					int off = layout.getOffsetForHorizontal(line, x);
					ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);
					for(ClickableSpan link: links) {
						linkList.add(link);
					}
				}

				if (linkList.size() > 0) {
					ClickableSpan link = linkList.get(linkList.size() - 1);
					if (action == MotionEvent.ACTION_UP && mIsPressed) {
						mIsPressed = false;
						link.onClick(textView);
						return dispatchTouch(true, v, event);
					} else if (action == MotionEvent.ACTION_DOWN) {
						mIsPressed = true;
						return dispatchTouch(true, v, event);
					}
				}

				mIsPressed = false;
			}

			return dispatchTouch(false, v, event);
		}

		private boolean dispatchTouch(boolean ret, View v, MotionEvent event) {
			if(onTouchListener != null) {
				return ret || onTouchListener.onTouch(v, event);
			}
			return ret;
		}
	}
}
