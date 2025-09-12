(ns clojure.gdx.scene2d.actor
  (:require [clojure.scene2d.event :as event]
            [clojure.gdx.scene2d.touchable :as touchable]
            [clojure.gdx.scene2d.utils :as utils])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.math Vector2)))

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

(defn- set-touchable! [^Actor actor touchable]
  (.setTouchable actor touchable))

(defn remove! [^Actor actor]
  (.remove actor))

(defn parent [^Actor actor]
  (.getParent actor))

(defn stage->local-coordinates [actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    [(.x v)
     (.y v)]))

(defn hit [actor [x y]]
  (.hit actor x y true))

(defn set-opts!
  [^Actor actor
   {:keys [id
           name
           user-object
           visible?
           center-position
           position] :as opts}]
  (assert (map? opts)
          (str "opts should be a map, given " (pr-str opts)))
  (when id
    (set-user-object! actor id))
  (when name
    (.setName actor name))
  (when user-object
    (set-user-object! actor user-object))
  (when (contains? opts :visible?)
    (set-visible! actor visible?))
  (when-let [[x y] center-position]
    (.setPosition actor
                  (- x (/ (.getWidth  actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (when-let [[x y] position]
    (.setPosition actor x y))
  (when-let [touchable (:actor/touchable opts)]
    (set-touchable! actor (touchable/k->value touchable)))
  (when-let [f (:click-listener opts)]
    (.addListener actor (utils/click-listener
                         (fn [event x y]
                           (f @(.ctx ^clojure.gdx.scene2d.Stage (event/stage event)))))))
  actor)

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))

(defmulti build :actor/type)

(defn build? ^Actor [actor-declaration]
  (cond
   (instance? Actor actor-declaration) actor-declaration
   (map? actor-declaration) (build actor-declaration) ; TODO build just returned the map -> post assertion instance? Actor
   (nil? actor-declaration) nil ; TODO why nil
   :else (throw (ex-info "Cannot find constructor"
                         {:instance-actor? (instance? Actor actor-declaration)
                          :map? (map? actor-declaration)}))))
