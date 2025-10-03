(ns com.badlogic.gdx.scenes.scene2d.actor
  (:require [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable])
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

(defn stage->local-coordinates [^Actor actor position]
  (-> actor
      (.stageToLocalCoordinates (vector2/->java position))
      vector2/->clj))

(defn hit [^Actor actor [x y]]
  (.hit actor x y true))

(defn set-name! [^Actor actor name]
  (.setName actor name))

(defn set-position! [^Actor actor x y]
  (.setPosition actor x y))

(defn get-width [^Actor actor]
  (.getWidth actor))

(defn get-height [^Actor actor]
  (.getHeight actor))

(defn add-listener! [^Actor actor listener]
  (.addListener actor listener))

(defn create
  [{:keys [actor/act
           actor/draw]}]
  (proxy [Actor] []
    (act [delta]
      (act this delta)
      (proxy-super act delta))
    (draw [batch parent-alpha]
      (draw this batch parent-alpha))))
