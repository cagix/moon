(ns clojure.gdx.scene2d.actor.opts
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [clojure.scene2d.event :as event])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(def fn-map
  {:id actor/set-user-object!
   :name (fn [actor name]
           (Actor/.setName actor name))
   :user-object actor/set-user-object!
   :visible? actor/set-visible!
   :position (fn [actor [x y]]
               (Actor/.setPosition actor x y))
   :center-position (fn [actor [x y]]
                      (.setPosition actor
                                    (- x (/ (.getWidth  actor) 2))
                                    (- y (/ (.getHeight actor) 2))))
   :actor/touchable actor/set-touchable!
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
