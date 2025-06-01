(ns clojure.gdx
  (:require [clojure.gdx.files :as files])
  (:import (com.badlogic.gdx Gdx)))

(defn app []
  Gdx/app)

(defn files []
  (let [this Gdx/files]
    (reify files/Files
      (internal [_ path]
        (.internal this path)))))

(defn graphics []
  Gdx/graphics)

(defn input []
  Gdx/input)
