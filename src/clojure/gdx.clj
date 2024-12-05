(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn exit []
  (.exit Gdx/app))

(defn post-runnable [runnable]
  (.postRunnable Gdx/app runnable))
