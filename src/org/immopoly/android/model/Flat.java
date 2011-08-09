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

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class Flat implements Parcelable, Comparable<Flat>, SQLData {

	// http://developer.immobilienscout24.de/wiki/Expose/GET
	public Integer uid;
	public String name;
	public String description;
	public String locationNote;
	public double lat;
	public double lng;
	
	public boolean displayed = false;
	public boolean owned = false;

	public int houseNumber;
	public String street;
	public String city;
	public String postcode;
	public double distance;

	public String quarter;
	public String titlePictureTitle;
	public String titlePictureDescriptionNote;
	public String titlePictureFurnitureNote;
	public String titlePictureLocationNote;
	public String titlePictureSmall = "";
	public String titlePictureMedium = "";
	public String titlePictureLarge = "";
	public String marketingType;
	public String priceValue;
	public String priceIntervaleType;
	public String currency;

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(uid);
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(city);
		dest.writeString(titlePictureSmall);
		dest.writeString(priceValue);
		dest.writeString(currency);
		dest.writeString(priceIntervaleType);
		dest.writeDouble(lat);
		dest.writeDouble(lng);
	}

	public static final Parcelable.Creator<Flat> CREATOR = new Parcelable.Creator<Flat>() {
		public Flat createFromParcel(Parcel in) {
			return new Flat(in);
		}

		public Flat[] newArray(int size) {
			return new Flat[size];
		}
	};

	public Flat(Parcel in) {
		uid = in.readInt();
		name = in.readString();
		description = in.readString();
		city = in.readString();
		titlePictureSmall = in.readString();
		priceValue = in.readString();
		currency = in.readString();
		priceIntervaleType = in.readString();
		lat = in.readDouble();
		lng = in.readDouble();
	}

	public Flat() {
	}

	public Flat(JSONObject jsonObject) throws JSONException {
		distance = jsonObject.optDouble("distance", 0);
		uid = jsonObject.optInt("realEstateId");
		// OWN OBJECT
		JSONObject objRealEstate = jsonObject
				.getJSONObject("resultlist.realEstate");
		name = objRealEstate.optString("title");
		description = objRealEstate.optString("descriptionNote");
		locationNote = objRealEstate.optString("locationNote");
		// OWN OBJECT

		/**
		 * Address Fields
		 */
		JSONObject objAddress = objRealEstate.getJSONObject("address");
		street = objAddress.optString("street");
		houseNumber = objAddress.optInt("houseNumber", 0);
		postcode = objAddress.optString("postcode");
		city = objAddress.optString("city");
		quarter = objAddress.optString("quater");
		if (objAddress != null && objAddress.has("wgs84Coordinate")) {
			JSONObject coordinate = objAddress.getJSONObject("wgs84Coordinate");
			lat = coordinate.optDouble("latitude");
			lng = coordinate.optDouble("longitude");
		}
		if (objRealEstate.has("titlePicture")) {
			JSONObject objPicture = objRealEstate.getJSONObject("titlePicture");
			titlePictureTitle = objPicture.optString("title");
			titlePictureDescriptionNote = objPicture
					.optString("descriptionNote");
			titlePictureFurnitureNote = objPicture.optString("furnishingNote");
			titlePictureLocationNote = objPicture.optString("locationNote");
			titlePictureSmall = objPicture.optString("@xlink.href");
			if (objPicture.has("urls")
					&& objPicture.getJSONArray("urls").length() > 0) {
				// JSONArray urls =
				// objPicture.getJSONArray("urls").getJSONObject(
				// 0).getJSONArray("url");
				JSONObject urls = objPicture.getJSONArray("urls")
						.getJSONObject(0).getJSONObject("url");
				titlePictureSmall = urls.optString("@href");
				/*
				 * if (urls.length() > 0) { titlePictureSmall =
				 * urls.getJSONObject(0).getString("href"); } else if
				 * (urls.length() > 1) { titlePictureMedium =
				 * urls.getJSONObject(1).getString("href"); } else if
				 * (urls.length() > 2) { titlePictureLarge =
				 * urls.getJSONObject(2).getString("href"); }
				 */
			}
		}
		if (objRealEstate.has("price")) {
			marketingType = objRealEstate.getJSONObject("price").optString(
					"marketingType");
			priceValue = objRealEstate.getJSONObject("price")
					.optString("value");
			priceIntervaleType = objRealEstate.getJSONObject("price")
					.optString("priceIntervalType");
			currency = objRealEstate.getJSONObject("price").optString(
					"currency");
		}
	}

	@Override
	public int compareTo(Flat another) {
		// TODO Auto-generated method stub
		return this.name.compareTo(another.name);
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return "de.tosa.findaflat.flat";
	}

	@Override
	public void readSQL(SQLInput arg0, String arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeSQL(SQLOutput arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

}