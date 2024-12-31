(ns clojure.gdx.scene2d.actor
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.math Vector2)))

(def x Actor/.getX)
(def y Actor/.getY)

(def parent Actor/.getParent)

(def set-id Actor/.setUserObject)

(def user-object Actor/.getUserObject)
(def set-visible Actor/.setVisible)
(def visible?    Actor/.isVisible)

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn set-center [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth  actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn set-opts [^Actor a {:keys [id name visible? touchable center-position position] :as opts}]
  (when id                          (.setUserObject        a id))
  (when name                        (.setName      a name))
  (when (contains? opts :visible?)  (.setVisible   a (boolean visible?)))
  (when touchable                   (.setTouchable a (case touchable
                                                       :children-only Touchable/childrenOnly
                                                       :disabled      Touchable/disabled
                                                       :enabled       Touchable/enabled)))
  (when-let [[x y] center-position] (set-center    a x y))
  (when-let [[x y] position]        (.setPosition  a x y))
  a)
