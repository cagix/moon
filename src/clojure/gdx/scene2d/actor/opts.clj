(ns clojure.gdx.scene2d.actor.opts
  (:require [clojure.scene2d.actor :as actor]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [clojure.scene2d.event :as event])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(def fn-map
  {:id (fn [actor object]
         (actor/set-user-object! actor object))
   :name (fn [actor name]
           (Actor/.setName actor name))
   :user-object (fn [actor object]
                  (actor/set-user-object! actor object))
   :visible? (fn [actor bool]
               (actor/set-visible! actor bool))
   :position (fn [actor [x y]]
               (Actor/.setPosition actor x y))
   :center-position (fn [actor [x y]]
                      (.setPosition actor
                                    (- x (/ (.getWidth  actor) 2))
                                    (- y (/ (.getHeight actor) 2))))
   :actor/touchable (fn [actor touchable]
                      (actor/set-touchable! actor touchable))
   :click-listener (fn [actor f]
                     (.addListener actor (listener/click
                                          (fn [event x y]
                                            (f @(.ctx ^clojure.gdx.scene2d.Stage (event/stage event)))))))})

(defn set-actor-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get fn-map k)]
          :when f]
    (f actor v))
  actor)
