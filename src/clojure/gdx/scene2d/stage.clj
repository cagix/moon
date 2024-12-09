(ns clojure.gdx.scene2d.stage
  (:require [clojure.gdx.scene2d.group :refer [find-actor-with-id]])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(def act  Stage/.act)
(def draw Stage/.draw)
(def root Stage/.getRoot)

(defn create
  "Actors or screen can be nil."
  [viewport batch actors]
  (let [stage (proxy [Stage clojure.lang.ILookup] [viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    stage))
