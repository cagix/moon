(ns forge.screens.stage
  (:require [anvil.graphics :refer [batch]]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.group :refer [find-actor-with-id]]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.app.gui-viewport :refer [gui-viewport gui-mouse-position]]
            [forge.app.screens :refer [current-screen]]
            [forge.screen :as screen])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

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
  (let [stage (proxy [Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (stage/root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (stage/root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage (or screen empty-screen))))

(defn screen-stage ^Stage []
  (:stage (current-screen)))

(defn add-actor [actor]
  (.addActor (screen-stage) actor))

(defn reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (gui-mouse-position)]
    (.hit (screen-stage) x y true)))
