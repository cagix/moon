(ns com.badlogic.gdx.scenes.scene2d.actor
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defprotocol Tooltip
  (add-tooltip! [actor tooltip-text])
  (remove-tooltip! [actor]))

(defn get-stage [^Actor actor]
  (.getStage actor))

(defn- get-ctx [actor]
  (when-let [stage (get-stage actor)]
    (stage/get-ctx stage)))

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

(def opts-fn-map
  {:actor/name set-name!
   :actor/user-object set-user-object!
   :actor/visible?  set-visible!
   :actor/touchable set-touchable!
   :actor/listener add-listener!
   :actor/position (fn [actor [x y]]
                     (set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (set-position! actor
                                           (- x (/ (get-width  actor) 2))
                                           (- y (/ (get-height actor) 2))))})

(defn set-opts! [^Actor actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn act! [^Actor actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [^Actor actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))

(defn- create-actor*
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
          (act [delta]
            (act this delta)
            (proxy-super act delta))
          (draw [batch parent-alpha]
            (draw this batch parent-alpha)))
    (set-opts! opts)))

(defmethod scene2d/build :actor.type/actor
  [opts]
  (create-actor*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when (:act opts)
                         (act! actor delta (:act opts))))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when (:draw opts)
                          (draw! actor (:draw opts)))))))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
