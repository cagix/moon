(ns clojure.scene2d.build.actor
  (:require [cdq.graphics :as graphics]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- get-ctx [actor]
  (when-let [stage (Actor/.getStage actor)]
    (.ctx stage)))

(defn act! [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [actor f]
  (when-let [ctx (get-ctx actor)]
    (graphics/draw! (:ctx/graphics ctx) (f actor ctx))))

(defmethod scene2d/build :actor.type/actor
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (actor/create
   {:act (fn [this delta]
           (when act
             (act! this delta act)))
    :draw (fn [this _batch _parent-alpha]
            (when draw
              (draw! this draw)))}
   opts))
