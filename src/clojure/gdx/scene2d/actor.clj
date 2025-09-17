(ns clojure.gdx.scene2d.actor
  (:require [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable]
            [com.badlogic.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Widget)))

(extend-type Actor
  clojure.scene2d.actor/Actor
  (get-stage [actor]
    (.getStage actor))

  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (touchable/k->value touchable)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor))

  (stage->local-coordinates [actor position]
    (-> actor
        (.stageToLocalCoordinates (vector2/->java position))
        vector2/->clj))

  (hit [actor [x y]]
    (.hit actor x y true))

  (set-name! [actor name]
    (.setName actor name))

  (set-position! [actor x y]
    (.setPosition actor x y))

  (get-width [actor]
    (.getWidth actor))

  (get-height [actor]
    (.getHeight actor))

  (add-listener! [actor listener]
    (.addListener actor listener)))

(def ^:private opts-fn-map
  {:actor/name actor/set-name!
   :actor/user-object actor/set-user-object!
   :actor/visible?  actor/set-visible!
   :actor/touchable actor/set-touchable!
   :actor/listener actor/add-listener!
   :actor/position (fn [actor [x y]]
                     (actor/set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (actor/set-position! actor
                                                 (- x (/ (actor/get-width  actor) 2))
                                                 (- y (/ (actor/get-height actor) 2))))})

(defn set-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn- create*
  [{:keys [actor/act
           actor/draw]
    :as opts}]
  (doto (proxy [Actor] []
          (act [delta] ; TODO call proxy super required ?-> fixes tooltips in pure scene2d?
            (act this delta))
          (draw [batch parent-alpha]
            (draw this batch parent-alpha)))
    (set-opts! opts)))

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn- try-act [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn- try-draw [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))

(defn create
  [{:keys [act draw]
    :as opts}]
  (create*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when act
                         (try-act actor delta act)))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when draw
                          (try-draw actor draw))))))

(defn create-widget [opts]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (when-let [f (:draw opts)]
        (try-draw this f)))))
