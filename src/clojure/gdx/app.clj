(ns clojure.gdx.app
  (:import (com.badlogic.gdx Gdx)))

(defn exit []
  (.exit Gdx/app))

(defn post-runnable [runnable]
  (.postrunnable Gdx/app runnable))
