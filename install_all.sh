#!/bin/bash
echo "Installing all flavors..."

echo "1. MVP..."
./gradlew installMvpDebug

echo "2. MVVM LiveData..."
./gradlew installMvvmLiveDataDebug

echo "3. MVI..."
./gradlew installMviDebug

echo "4. MVVM Coroutines..."
./gradlew installMvvmCoroutinesDebug

echo "5. MVVM Flow..."
./gradlew installMvvmFlowDebug

echo "6. Machine Flavor..."
./gradlew installMachineDebug

echo "All flavors installed!"
