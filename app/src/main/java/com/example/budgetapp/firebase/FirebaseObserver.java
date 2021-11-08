package com.example.budgetapp.firebase;

public interface FirebaseObserver<T> {
    void onChanged(T t);
}
