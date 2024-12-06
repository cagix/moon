(ns forge.app.screens
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.group :refer [find-actor-with-id]]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.app.gui-viewport :refer [gui-viewport
                                            gui-mouse-position]]
            [forge.app.sprite-batch :refer [batch]]
            [forge.utils :refer [bind-root mapvals]]))

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (screen-destroy [_]))

(declare ^:private screens
         ^:private current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root current-screen-key new-k)
    (enter screen)))

(defn screen-stage ^com.badlogic.gdx.scenes.scene2d.Stage []
  (:stage (current-screen)))

(defn add-actor [actor]
  (.addActor (screen-stage) actor))

(defn reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (gui-mouse-position)]
    (.hit (screen-stage) x y true)))

(defrecord StageScreen [stage sub-screen]
  Screen
  (enter [_]
    (input/set-processor stage)
    (enter sub-screen))

  (exit [_]
    (input/set-processor nil)
    (exit sub-screen))

  (render [_]
    (stage/act stage)
    (render sub-screen)
    (stage/draw stage))

  (screen-destroy [_]
    (dispose stage)
    (screen-destroy sub-screen)))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [com.badlogic.gdx.scenes.scene2d.Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (stage/root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (stage/root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn create [[_ {:keys [ks first-k]}]]
  (bind-root screens (mapvals stage-screen (mapvals
                                            (fn [ns-sym]
                                              (require ns-sym)
                                              ((ns-resolve ns-sym 'create)))
                                            ks)))
  (change-screen first-k))

(defn destroy [_]
  (run! screen-destroy (vals screens)))

(defn render [_]
  (g/clear-screen color/black)
  (render (current-screen)))
