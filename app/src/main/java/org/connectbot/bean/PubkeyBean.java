/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.bean;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import org.connectbot.R;
import org.connectbot.util.PubkeyDatabase;
import org.connectbot.util.PubkeyUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;

/**
 * @author Kenny Root
 *
 */
public class PubkeyBean extends AbstractBean {
	public static final String BEAN_NAME = "pubkey";

	/* Database fields */
	private long id;
	private String nickname;
	private String type;
	private byte[] privateKey;
	private byte[] publicKey;
	private boolean encrypted = false;
	private boolean startup = false;
	private boolean confirmUse = false;
	private boolean securityKey = false;
	private String securityKeyType;
	private int lifetime = 0;

	/* Transient values */
	private transient boolean unlocked = false;
	private transient Object unlockedPrivate = null;
	private transient Integer bits;

	@Override
	public String getBeanName() {
		return BEAN_NAME;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getNickname() {
		return nickname;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setPrivateKey(byte[] privateKey) {
		if (privateKey == null)
			this.privateKey = null;
		else
			this.privateKey = privateKey.clone();
	}

	public byte[] getPrivateKey() {
		if (privateKey == null)
			return null;
		else
			return privateKey.clone();
	}

	public void setPublicKey(byte[] encoded) {
		if (encoded == null)
			publicKey = null;
		else
			publicKey = encoded.clone();
	}

	public byte[] getPublicKey() {
		if (publicKey == null)
			return null;
		else
			return publicKey.clone();
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public void setStartup(boolean startup) {
		this.startup = startup;
	}

	public boolean isStartup() {
		return startup;
	}

	public void setConfirmUse(boolean confirmUse) {
		this.confirmUse = confirmUse;
	}

	public boolean isSecurityKey() {
		return securityKey;
	}

	public void setSecurityKey(boolean securityKey) {
		this.securityKey = securityKey;
	}

	public void setSecurityKeyType(String securityKeyType) {
		this.securityKeyType = securityKeyType;
	}

	public String getSecurityKeyType() {
		return securityKeyType;
	}

	public boolean isConfirmUse() {
		return confirmUse;
	}

	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}

	public int getLifetime() {
		return lifetime;
	}

	public void setUnlocked(boolean unlocked) {
		this.unlocked = unlocked;
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public void setUnlockedPrivate(Object unlockedPrivate) {
		this.unlockedPrivate = unlockedPrivate;
	}

	public Object getUnlockedPrivate() {
		return unlockedPrivate;
	}

	public String getDescription(Context context) {
		if (bits == null) {
			try {
				bits = PubkeyUtils.getBitStrength(publicKey, type);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {
			}
		}

		Resources res = context.getResources();
		final StringBuilder sb = new StringBuilder();
		if (PubkeyDatabase.KEY_TYPE_RSA.equals(type)) {
			sb.append(res.getString(R.string.key_type_rsa_bits, bits));
		} else if (PubkeyDatabase.KEY_TYPE_DSA.equals(type)) {
			sb.append(res.getString(R.string.key_type_dsa_bits, 1024));
		} else if (PubkeyDatabase.KEY_TYPE_EC.equals(type)) {
			sb.append(res.getString(R.string.key_type_ec_bits, bits));
		} else if (PubkeyDatabase.KEY_TYPE_ED25519.equals(type)) {
			sb.append(res.getString(R.string.key_type_ed25519));
		} else {
			sb.append(res.getString(R.string.key_type_unknown));
		}

		if (encrypted) {
			sb.append(' ');
			sb.append(res.getString(R.string.key_attribute_encrypted));
		}

		if (securityKey) {
			sb.append(' ');
			sb.append(res.getString(R.string.key_attribute_hardware));
			sb.append(" (" + securityKeyType + ")");
		}

		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.connectbot.bean.AbstractBean#getValues()
	 */
	@Override
	public ContentValues getValues() {
		ContentValues values = new ContentValues();

		values.put(PubkeyDatabase.FIELD_PUBKEY_NICKNAME, nickname);
		values.put(PubkeyDatabase.FIELD_PUBKEY_TYPE, type);
		values.put(PubkeyDatabase.FIELD_PUBKEY_PRIVATE, privateKey);
		values.put(PubkeyDatabase.FIELD_PUBKEY_PUBLIC, publicKey);
		values.put(PubkeyDatabase.FIELD_PUBKEY_ENCRYPTED, encrypted ? 1 : 0);
		values.put(PubkeyDatabase.FIELD_PUBKEY_STARTUP, startup ? 1 : 0);
		values.put(PubkeyDatabase.FIELD_PUBKEY_CONFIRMUSE, confirmUse ? 1 : 0);
		values.put(PubkeyDatabase.FIELD_PUBKEY_LIFETIME, lifetime);
		values.put(PubkeyDatabase.FIELD_PUBKEY_SECURITYKEY, securityKey ? 1 : 0);
		values.put(PubkeyDatabase.FIELD_PUBKEY_SECURITYKEYTYPE, securityKeyType);

		return values;
	}

	public boolean changePassword(String oldPassword, String newPassword) throws Exception {
		PrivateKey priv;

		try {
			priv = PubkeyUtils.decodePrivate(getPrivateKey(), getType(), oldPassword);
		} catch (Exception e) {
			return false;
		}

		setPrivateKey(PubkeyUtils.getEncodedPrivate(priv, newPassword));
		setEncrypted(newPassword.length() > 0);

		return true;
	}
}
