(ns clojure.gdx.scene2d.stage
  (:require [clojure.gdx.scene2d.group :as group])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn root [^Stage stage]
  (Stage/.getRoot stage))

(defn create [viewport batch]
  (proxy [Stage ILookup] [viewport batch]
    (valAt
      ([id]
       (group/find-actor-with-id (root this) id))
      ([id not-found]
       (or (group/find-actor-with-id (root this) id)
           not-found)))))

(defn hit [^Stage stage [x y]]
  (.hit stage x y true))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))

(defn draw! [^Stage stage]
  (.draw stage))

(defn act! [^Stage stage]
  (.act stage))
