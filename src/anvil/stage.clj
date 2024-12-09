(ns anvil.stage
  (:refer-clojure :exclude [get])
  (:require [anvil.app :as app]
            [anvil.graphics :as g]
            [anvil.screen :as screen]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.group :refer [find-actor-with-id]]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :refer [dispose]])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn get []
  (:stage (app/current-screen)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (g/gui-mouse-position)]
    (.hit (get) x y true)))

(defrecord StageScreen [stage sub-screen]
  screen/Screen
  (enter [_]
    (input/set-processor stage)
    (screen/enter sub-screen))

  (exit [_]
    (input/set-processor nil)
    (screen/exit sub-screen))

  (render [_]
    (stage/act stage)
    (screen/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (dispose stage)
    (screen/dispose sub-screen)))

(def ^:private empty-screen
  (reify screen/Screen
    (enter [_])
    (exit [_])
    (render [_])
    (dispose [_])))

(defn create
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [Stage clojure.lang.ILookup] [g/gui-viewport g/batch]
                (valAt
                  ([id]
                   (find-actor-with-id (stage/root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (stage/root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage (or screen empty-screen))))
