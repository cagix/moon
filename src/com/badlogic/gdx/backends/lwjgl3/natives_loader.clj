(ns com.badlogic.gdx.backends.lwjgl3.natives-loader
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3NativesLoader)))

(defn load! []
  (Lwjgl3NativesLoader/load))
