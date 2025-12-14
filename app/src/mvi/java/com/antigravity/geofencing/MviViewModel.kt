package com.antigravity.geofencing

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MviViewModel : ViewModel() {

    private val intentsSubject: PublishSubject<MviIntent> = PublishSubject.create()
    private val statesSubject: PublishSubject<MviViewState> = PublishSubject.create()
    private val disposables = CompositeDisposable()

    // Simulate a repository or interact with GeofencingClient outside the stream pureness for
    // simplicity
    // Ideally we would inject a UseCase that returns Observables.

    // Public Observable for the View to subscribe to
    val states: Observable<MviViewState> = statesSubject.hide()

    init {
        val initialState = MviViewState()

        disposables.add(
                intentsSubject
                        .subscribeOn(Schedulers.io())
                        .flatMap { intent -> actionToResult(intent) }
                        .scan(initialState) { oldState, result -> reduce(oldState, result) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { state -> statesSubject.onNext(state) },
                                { error ->
                                    // Crash safety
                                    error.printStackTrace()
                                }
                        )
        )
    }

    fun processIntent(intent: MviIntent) {
        intentsSubject.onNext(intent)
    }

    private fun actionToResult(intent: MviIntent): Observable<MviResult> {
        return when (intent) {
            is MviIntent.SetGeofence -> {
                // Here we would effectively call the GeofencingClient
                // For MVI cleanliness, we should ideally Wrap it.
                // We'll simulate the "Side Effect" here or assume we have a way.
                // In a real app we'd map to a SideEffect/Action
                Observable.just<MviResult>(MviResult.Processing)
                        .concatWith(
                                // Fake async work or real logic
                                Observable.just<MviResult>(
                                                MviResult.SetGeofenceSuccess(
                                                        "Geofence Set: ${intent.lat}, ${intent.lng}"
                                                )
                                        )
                                        .flatMap { result ->
                                            // Side effect: Save to DB
                                            val entity =
                                                    com.antigravity.geofencing.data.GeofenceEntity(
                                                            requestId = intent.requestId,
                                                            latitude = intent.lat,
                                                            longitude = intent.lng,
                                                            radius = intent.radius
                                                    )

                                            // Use Observable.fromCallable/create to do DB op on IO
                                            Observable.fromCallable {
                                                        kotlinx.coroutines.runBlocking {
                                                            com.antigravity.geofencing.data
                                                                    .GeofenceRepository.getDao()
                                                                    .insert(entity)
                                                        }
                                                        result // Pass original result downstream
                                                    }
                                                    .subscribeOn(Schedulers.io())
                                        }
                                        .delay(500, TimeUnit.MILLISECONDS)
                        )
            }
            is MviIntent.SilenceAlarm -> Observable.just(MviResult.AlarmSilenced)
            is MviIntent.SnoozeAlarm -> {
                Observable.just<MviResult>(MviResult.AlarmSnoozed)
                        .concatWith(
                                Observable.timer(
                                                1, // 1 minute for demo? logic says 10s in comment
                                                TimeUnit.MINUTES
                                        )
                                        .map { MviResult.AlarmTriggered }
                                        .cast(MviResult::class.java)
                        )
            }
            is MviIntent.GeofenceEntered -> Observable.just(MviResult.AlarmTriggered)
            is MviIntent.ViewHistory -> Observable.just(MviResult.NavigateToHistory(true))
            is MviIntent.HistoryNavigated -> Observable.just(MviResult.NavigateToHistory(false))
        }
    }

    private fun reduce(previousState: MviViewState, result: MviResult): MviViewState {
        return when (result) {
            is MviResult.Processing -> previousState.copy(isLoading = true, error = null)
            is MviResult.SetGeofenceSuccess ->
                    previousState.copy(isLoading = false, status = result.message)
            is MviResult.Failure -> previousState.copy(isLoading = false, error = result.error)
            is MviResult.AlarmTriggered ->
                    previousState.copy(isAlarmPlaying = true, status = "ALARM! Entered Zone.")
            is MviResult.AlarmSilenced ->
                    previousState.copy(isAlarmPlaying = false, status = "Silenced.")
            is MviResult.AlarmSnoozed ->
                    previousState.copy(isAlarmPlaying = false, status = "Snoozed...")
            is MviResult.NavigateToHistory ->
                    previousState.copy(navigateToHistory = result.navigate)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
