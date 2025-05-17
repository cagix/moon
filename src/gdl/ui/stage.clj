(ns gdl.ui.stage
  (:require [gdl.ui :as ui])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn add-actor! [stage actor]
  (Stage/.addActor stage actor))

(defn draw! [stage]
  (Stage/.draw stage))

(defn act! [stage]
  (Stage/.act stage))

(defn root [stage]
  (Stage/.getRoot stage))

(defn hit [^Stage stage [x y]]
  (.hit stage x y true))

(defn create [viewport batch actors]
  (let [stage (proxy [Stage ILookup] [viewport batch]
                (valAt [id]
                  (ui/find-actor-with-id (root this) id)))]
    (run! (partial add-actor! stage) actors)
    stage))

