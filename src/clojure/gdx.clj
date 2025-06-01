(ns clojure.gdx
  (:require [clojure.gdx.app :as app]
            [clojure.gdx.files :as files])
  (:import (com.badlogic.gdx Gdx)))

(defn app []
  (let [this Gdx/app]
    (reify app/App
      (post-runnable! [_ runnable]
        (.postRunnable this runnable)))))

(defn files []
  (let [this Gdx/files]
    (reify files/Files
      (internal [_ path]
        (.internal this path)))))

(defn graphics []
  Gdx/graphics)

(defn input []
  Gdx/input)
