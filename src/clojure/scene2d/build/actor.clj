(ns clojure.scene2d.build.actor
  (:require [cdq.graphics.draws :as draws]
            [clojure.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- get-ctx [actor]
  (when-let [stage (Actor/.getStage actor)]
    (.ctx stage)))

(defn act! [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [actor f]
  (when-let [ctx (get-ctx actor)]
    (draws/handle! (:ctx/graphics ctx) (f actor ctx))))

(def opts-fn-map
  {:actor/name        Actor/.setName
   :actor/user-object Actor/.setUserObject
   :actor/visible?    Actor/.setVisible
   :actor/touchable   Actor/.setTouchable
   :actor/listener    Actor/.addListener
   :actor/position (fn [a [x y]]
                     (Actor/.setPosition a x y))
   :actor/center-position (fn [a [x y]]
                            (Actor/.setPosition a
                                                 (- x (/ (Actor/.getWidth  a) 2))
                                                 (- y (/ (Actor/.getHeight a) 2))))})

(defn set-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defmethod scene2d/build :actor.type/actor
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
         (act [delta]
           (when act
             (act! this delta act))
           (proxy-super act delta))
         (draw [_batch _parent-alpha]
           (when draw
             (draw! this draw))))
    (set-opts! opts)))
