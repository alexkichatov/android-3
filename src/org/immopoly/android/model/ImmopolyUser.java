/*
 * This is the Android component of Immopoly
 * http://immopoly.appspot.com
 * Copyright (C) 2011 Tobias Sasse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package org.immopoly.android.model;

import java.util.ArrayList;
import java.util.List;

import org.immopoly.common.History;
import org.immopoly.common.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class ImmopolyUser extends User {

	public static final String sPREF_USER = "user";
	public static final String sPREF_TOKEN = "user_token";

	private String mUserToken;
	private String mUserName;
	private double mBalance;
	public List<ImmopolyHistory> mUserHistory;
	public List<Flat> flats;
	private double sLastProvision;
	private double sLastRent;
	private static ImmopolyUser sInstance = null;

	private ImmopolyUser() {
		mUserHistory = new ArrayList<ImmopolyHistory>();
		flats = new ArrayList<Flat>();
	}

	@Override
	public double getBalance() {
		// TODO Auto-generated method stub
		return mBalance;
	}

	public void readToken(Context context) {
		if (mUserToken == null || mUserToken.length() == 0) {
			SharedPreferences shared = context.getSharedPreferences(sPREF_USER,
					0);
			String userToken = shared.getString(sPREF_TOKEN, "");
			mUserToken = userToken;
		}
	}

	@Override
	public String getToken() {
		return mUserToken;
	}

	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		return mUserName;
	}

	@Override
	public void setBalance(double balance) {
		mBalance = balance;
	}

	@Override
	public void setToken(String token) {
		mUserToken = token;
	}

	@Override
	public void setUsername(String username) {
		// TODO Auto-generated method stub
		mUserName = username;
	}

	@Override
	public JSONObject toJSON() {
		return null;
	}

	public static ImmopolyUser getInstance() {
		if (sInstance == null) {
			sInstance = new ImmopolyUser();
		}
		return sInstance;
	}

	@Override
	public void setPortfolio(JSONObject portfolio) {
		if (portfolio != null) {

			JSONArray results;
			JSONArray resultEntries;
			try {
				Flat item;
				results = portfolio.getJSONArray("resultlistEntries");
				resultEntries = results.getJSONArray(0);

				JSONObject expose;
				JSONObject realEstate;
				flats = new ArrayList<Flat>();
				for (int i = 0; i < resultEntries.length(); i++) {
					item = new Flat();
					expose = resultEntries.getJSONObject(i).getJSONObject(
							"expose.expose");
					realEstate = expose.getJSONObject("realEstate");
					item.priceValue = realEstate.optString("baseRent");
					item.name = realEstate.optString("title");
					item.uid = realEstate.optInt("@id");
					item.lat = realEstate.getJSONObject("address")
							.getJSONObject("wgs84Coordinate")
							.optDouble("longitude");
					item.lng = realEstate.getJSONObject("address")
							.getJSONObject("wgs84Coordinate")
							.optDouble("latitude");
					flats.add(item);

				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public History instantiateHistory() {
		return new ImmopolyHistory();
	}

	@Override
	public void setHistory(List<History> history) {
		mUserHistory.clear();
		for (History history2 : history) {
			mUserHistory.add((ImmopolyHistory) history2);
		}

	}

	@Override
	public void setLastProvision(double lastProvision) {
		sLastProvision = lastProvision;
	}

	@Override
	public void setLastRent(double lastRent) {
		// TODO Auto-generated method stub
		sLastRent = lastRent;
	}

	public double getLastProvision() {
		return sLastProvision;
	}

	public double getLastRent() {
		return sLastRent;
	}

}