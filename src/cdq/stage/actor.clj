(ns cdq.stage.actor
  (:refer-clojure :exclude [name])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.math Vector2)))

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

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))

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

(defn hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn set-center! [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth  actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn set-opts! [^Actor actor {:keys [id
                                      name
                                      visible?
                                      touchable
                                      center-position
                                      position] :as opts}]
  (when id (set-user-object! actor id))
  (when name (set-name! actor name))
  (when (contains? opts :visible?) (set-visible! actor (boolean visible?)))
  (when-let [[x y] center-position] (set-center! actor x y))
  (when-let [[x y] position] (.setPosition actor x y))
  actor)
