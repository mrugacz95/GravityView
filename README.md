# GravityView for Android

This library provides GravityView which is Android ViewGroup with basic physics engine to simulate collisions between it's child
views!

# Example

Care about lifecycle in your activity:

```kotlin
class MainActivity : AppCompatActivity() {
    // ...

    override fun onPause() {
        super.onPause()
        gravityview.stop()
    }

    override fun onResume() {
        super.onResume()
        gravityview.start()
    }
}
```

Add GravityView in xml:

```xml

<pl.mrugacz95.gravityview.GravityView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/gravityview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layout_constraintTop_toTopOf="parent"
    app:gravity="9.81">

    <Button android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="65dp"
        android:layout_marginStart="50dp"
        android:text="Hello World!" />

    <TextView android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="5dp"
        android:layout_marginStart="50dp"
        android:text="Hello World!" />

</pl.mrugacz95.gravityview.GravityView>

```

Add more view to your needs:

<img src="https://user-images.githubusercontent.com/12548284/201971467-b7824334-f88a-4f35-be88-e8ce50476aa5.gif" width="292" height="519" />
