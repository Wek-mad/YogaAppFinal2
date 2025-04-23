package com.universalyoga.admin;

import java.util.List;

public interface FirestoreFetchCallback<T> {
		void onSuccess(List<T> data);
		void onFailure(Exception e);
}
