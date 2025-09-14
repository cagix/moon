(ns clojure.gdx.scene2d.actor
  (:require [clojure.gdx.scene2d.touchable :as touchable]
            [clojure.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn get-stage [^Actor actor]
  (.getStage actor))

(defn get-x [^Actor actor]
  (.getX actor))

(defn get-y [^Actor actor]
  (.getY actor))

(defn get-name [^Actor actor]
  (.getName actor))

(defn user-object [^Actor actor]
  (.getUserObject actor))

(defn set-user-object! [^Actor actor object]
  (.setUserObject actor object))

(defn visible? [^Actor actor]
  (.isVisible actor))

(defn set-visible! [^Actor actor visible?]
  (.setVisible actor visible?))

(defn set-touchable! [^Actor actor touchable]
  (.setTouchable actor (touchable/k->value touchable)))

(defn remove! [^Actor actor]
  (.remove actor))

(defn parent [^Actor actor]
  (.getParent actor))

(defn stage->local-coordinates [actor position]
  (-> actor
      (.stageToLocalCoordinates (vector2/->java position))
      vector2/->clj))

(defn hit [actor [x y]]
  (.hit actor x y true))

(defn set-name! [actor name]
  (Actor/.setName actor name))

(defn set-position! [actor x y]
  (Actor/.setPosition actor x y))

(defn get-width [actor]
  (Actor/.getWidth actor))

(defn get-height [actor]
  (Actor/.getHeight actor))

(defn add-listener! [actor listener]
  (.addListener actor listener))

(let [opts-fn-map {
                   :actor/name set-name!
                   :actor/user-object set-user-object!
                   :actor/visible?  set-visible!
                   :actor/touchable set-touchable!
                   :actor/listener add-listener!
                   :actor/position (fn [actor [x y]]
                                     (set-position! actor x y))
                   :actor/center-position (fn [actor [x y]]
                                            (set-position! actor
                                                           (- x (/ (get-width  actor) 2))
                                                           (- y (/ (get-height actor) 2))))
                   }]
  (defn set-opts! [actor opts]
    (doseq [[k v] opts
            :let [f (get opts-fn-map k)]
            :when f]
      (f actor v))
    actor))
