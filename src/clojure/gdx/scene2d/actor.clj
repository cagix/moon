(ns clojure.gdx.scene2d.actor
  (:refer-clojure :exclude [name])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)))

(defn create ^Actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw
        (draw this)))
    (act [_delta]
      (when act
        (act this)))))

(defn remove!
  "Removes this actor from its parent, if it has a parent."
  [^Actor actor]
  (.remove actor))

(defn user-object [^Actor actor]
  (Actor/.getUserObject actor))

(defn set-user-object! [^Actor actor object]
  (Actor/.setUserObject actor object))

(defn set-touchable! [^Actor actor touchable]
  (.setTouchable actor (case touchable
                         :disabled Touchable/disabled)))

(defn visible? [^Actor actor]
  (.isVisible actor))

(defn set-visible! [^Actor actor visible?]
  (.setVisible actor visible?))

(defn name [^Actor actor]
  (.getName actor))

(defn set-name! [^Actor actor name]
  (.setName actor name))

(defn parent [^Actor actor]
  (.getParent actor))

(defn x [^Actor actor]
  (.getX actor))

(defn y [^Actor actor]
  (.getY actor))
