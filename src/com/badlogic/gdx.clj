(ns com.badlogic.gdx
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx Gdx)))

(defrecord Context []
  gdx/Audio
  (sound [{:keys [clojure.gdx/audio
                  clojure.gdx/files]}
          path]
    (.newSound audio (.internal files path))))

(defn context []
  (map->Context
   {:clojure.gdx/audio    Gdx/audio
    :clojure.gdx/files    Gdx/files
    :clojure.gdx/graphics Gdx/graphics
    :clojure.gdx/input    Gdx/input}))
