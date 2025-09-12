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

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
