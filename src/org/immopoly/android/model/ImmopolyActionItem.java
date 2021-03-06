package org.immopoly.android.model;

import org.immopoly.common.ActionItem;
import org.json.JSONException;
import org.json.JSONObject;

public class ImmopolyActionItem extends ActionItem {

	// mandatory
	private int type;
	private String text;
	private String description;
	private String url;
	private int amount;

	public ImmopolyActionItem(JSONObject o) {
		super(o);
	}

	/**
	 * Wird im Client nicht mehr serialisiert.
	 */
	@Override
	public JSONObject toJSON() throws JSONException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public void setAmount(int amount) {
		this.amount = amount;
	}

	@Override
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void setText(String text) {
		String[] split = text.split(":");
		this.text = split[0];
		description = split[1];
	}

	public String getText() {
		return text;
	}

	public String getDescription() {
		return description;
	}

	public String getImageUrl() {
		return url;
	}

	public int getAmount() {
		return amount;
	}

	public int getType() {
		return type;
	}

	public void removeAmount(int i) {
		amount = amount - i;
	}
}
