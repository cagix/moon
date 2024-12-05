(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn exit []
  (.exit Gdx/app))

(defn post-runnable [runnable]
  (.postRunnable Gdx/app runnable))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn sprite-batch []
  (SpriteBatch.))
