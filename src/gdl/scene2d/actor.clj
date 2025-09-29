(ns gdl.scene2d.actor
  (:require [gdl.scene2d :as scene2d]
            [gdl.scene2d.ctx :as ctx]
            [gdl.scene2d.stage :as stage]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [com.badlogic.gdx.scenes.scene2d.touchable :as touchable])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defprotocol PActor
  (act [_ delta f])
  (draw [_ f])
  (get-stage [_])
  (get-x [_])
  (get-y [_])
  (get-name [_])
  (get-width [_])
  (get-height [_])
  (user-object [_])
  (set-name! [_ name])
  (set-position! [_ x y])
  (set-user-object! [_ object])
  (visible? [_])
  (set-visible! [_ visible?])
  (set-touchable! [_ touchable])
  (remove! [_])
  (parent [_])
  (stage->local-coordinates [_ position])
  (hit [_ [x y]])
  (add-listener! [_ listener])
  (set-opts! [_ opts]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))

(defprotocol Tooltip
  (add-tooltip! [_ tooltip-text-or-text-fn])
  (remove-tooltip! [_]))

(defn- get-ctx [actor]
  (when-let [stage (get-stage actor)]
    (stage/get-ctx stage)))

(def ^:private opts-fn-map
  {:actor/name (fn [a v] (set-name! a v))
   :actor/user-object (fn [a v] (set-user-object! a v))
   :actor/visible?  (fn [a v] (set-visible! a v))
   :actor/touchable (fn [a v] (set-touchable! a v))
   :actor/listener (fn [a v] (add-listener! a v))
   :actor/position (fn [actor [x y]]
                     (set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (set-position! actor
                                                 (- x (/ (get-width  actor) 2))
                                                 (- y (/ (get-height actor) 2))))})

(extend-type Actor
  PActor
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
    (.addListener actor listener))

  (set-opts! [actor opts]
    (doseq [[k v] opts
            :let [f (get opts-fn-map k)]
            :when f]
      (f actor v))
    actor)

  (act [actor delta f]
    (when-let [ctx (get-ctx actor)]
      (f actor delta ctx)))

  (draw [actor f]
    (when-let [ctx (get-ctx actor)]
      (ctx/draw! ctx (f actor ctx)))))

(defn- create*
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
  (create*
   (assoc opts
          :actor/act (fn [actor delta]
                       (when (:act opts)
                         (act actor delta (:act opts))))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when (:draw opts)
                          (draw actor (:draw opts)))))))
