(ns forge.stage
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :as utils]
            [forge.app :as app]
            [forge.graphics :refer [gui-mouse-position gui-viewport batch]]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defrecord StageScreen [stage sub-screen]
  app/Screen
  (enter [_]
    (gdx/set-input-processor stage)
    (app/enter sub-screen))

  (exit [_]
    (gdx/set-input-processor nil)
    (app/exit sub-screen))

  (render [_]
    (stage/act stage)
    (app/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (utils/dispose stage)
    (app/dispose sub-screen)))

(defn- stage-create [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (.getRoot this) id)
           not-found)))))

(defn create
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create gui-viewport batch)]
    (run! #(stage/add stage %) actors)
    (->StageScreen stage screen)))

(defn mouse-on-actor? []
  (stage/hit (app/stage) (gui-mouse-position) :touchable? true))

(defn add-actor [actor]
  (stage/add (app/stage) actor))

(defn reset [new-actors]
  (stage/clear (app/stage))
  (run! add-actor new-actors))
