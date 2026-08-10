package com.dcim.asset;

public class AssetApplyException extends RuntimeException {

	public AssetApplyException(String message) {
		super(message);
	}

	public AssetApplyException(String message, Throwable cause) {
		super(message, cause);
	}
}
