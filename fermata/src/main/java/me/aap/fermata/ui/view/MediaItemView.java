package me.aap.fermata.ui.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.concurrent.Future;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.lib.MediaLib.PlayableItem;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.menu.AppMenu;

/**
 * @author Andrey Pavlenko
 */
public class MediaItemView extends ConstraintLayout implements OnLongClickListener,
		OnCheckedChangeListener {
	private static final RotateAnimation rotate = new RotateAnimation(0, 360,
			Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
	private final ColorStateList iconTint;
	private MediaItemWrapper itemWrapper;
	private Future<Void> loading;
	private MediaItemListView listView;

	public MediaItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(getContext(), R.layout.media_item_layout, this);
		iconTint = getIcon().getImageTintList();
		setLongClickable(true);
		setOnLongClickListener(this);
		getCheckBox().setOnCheckedChangeListener(this);
	}

	public MediaItemWrapper getItemWrapper() {
		return itemWrapper;
	}

	public Item getItem() {
		return getItemWrapper().getItem();
	}

	public void setItemWrapper(MediaItemWrapper wrapper) {
		if (loading != null) {
			loading.cancel(false);
			loading = null;
		}

		itemWrapper = wrapper;
		wrapper.setView(this);
		Item i = wrapper.getItem();

		if (i instanceof PlayableItem) {
			PlayableItem pi = (PlayableItem) i;

			if (pi.isMediaDataLoaded()) {
				setDescription(pi, pi.getMediaData().getDescription());
			} else {
				getTitle().setText(pi.getFile().getName());
				getSubtitle().setText(R.string.loading);
				ImageView icon = getIcon();
				icon.setImageResource(R.drawable.loading);
				rotate.setDuration(1000);
				rotate.setRepeatCount(Animation.INFINITE);
				icon.startAnimation(rotate);
				loading = pi.getMediaData(m -> {
					if (wrapper == getItemWrapper()) setDescription(pi, m.getDescription());
				});
			}
		} else {
			setDescription(i, i.getMediaDescription());
		}
	}

	public ImageView getIcon() {
		return (ImageView) getChildAt(0);
	}

	public TextView getTitle() {
		return (TextView) getChildAt(1);
	}

	public TextView getSubtitle() {
		return (TextView) getChildAt(2);
	}

	public MaterialCheckBox getCheckBox() {
		return (MaterialCheckBox) getChildAt(3);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		getItemWrapper().setSelected(isChecked, false);
	}

	public MediaItemListView getListView() {
		return listView;
	}

	public void setListView(MediaItemListView listView) {
		this.listView = listView;
	}

	private void setDescription(Item item, MediaDescriptionCompat dsc) {
		Bitmap img = dsc.getIconBitmap();
		ImageView icon = getIcon();
		icon.clearAnimation();

		if (img != null) {
			icon.setImageBitmap(img);
			icon.setImageTintList(null);
		} else {
			Uri uri = dsc.getIconUri();

			if (uri != null) {
				icon.setImageURI(uri);
				icon.setImageTintList(null);
			} else {
				icon.setImageTintList(iconTint);
				icon.setImageResource(item.getIcon());
			}
		}

		getTitle().setText(item.getTitle());
		getSubtitle().setText(item.getSubtitle());
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		Item item = getItem();

		if ((visibility == VISIBLE) && (item != null)) {
			refreshState();
		}
	}

	public void refresh() {
		setItemWrapper(getItemWrapper());
		refreshState();
	}

	public void refreshState() {
		Item item = getItem();
		int type = ((item instanceof PlayableItem) && ((PlayableItem) item).isLastPlayed()) ?
				Typeface.BOLD : Typeface.NORMAL;
		getTitle().setTypeface(null, type);
		getSubtitle().setTypeface(null, type);
		refreshCheckbox();

		if (item.equals(getMainActivity().getCurrentPlayable())) {
			setSelected(true);
			requestFocus();
		} else {
			setSelected(false);
		}
	}

	public void refreshCheckbox() {
		MediaItemWrapper w = getItemWrapper();
		MaterialCheckBox cb = getCheckBox();

		if (!w.isSelectionSupported()) {
			cb.setVisibility(GONE);
			return;
		}

		if (getListView().isSelectionActive()) {
			cb.setVisibility(VISIBLE);
			cb.setChecked(w.isSelected());
		} else {
			cb.setVisibility(GONE);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		MainActivityDelegate a = getMainActivity();
		AppMenu menu = a.getContextMenu();
		MediaItemMenuHandler handler = new MediaItemMenuHandler(menu, getItem());
		getListView().discardSelection();
		handler.show(R.layout.media_item_menu);
		return true;
	}

	void hideMenu() {
		AppMenu menu = getMainActivity().findViewById(R.id.context_menu);
		menu.hide();
	}

	private MainActivityDelegate getMainActivity() {
		return MainActivityDelegate.get(getContext());
	}
}