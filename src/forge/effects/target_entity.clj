(ns forge.effects.target-entity
  (:require [anvil.graphics :as g]
            [clojure.gdx.math.vector2 :as v]
            [forge.app.db :as db]
            [forge.effect :refer [effects-applicable? effects-do!]]
            [forge.entity.body :refer [e-direction]]
            [forge.world :refer [spawn-line-render spawn-audiovisual]]))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (e-direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (e-direction entity target*)
                  maxrange)))

(defn applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
  (and target
       (effects-applicable? ctx entity-effects)))

(defn useful? [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
  (in-range? @source @target maxrange))

(defn handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx}]
  (let [source* @source
        target* @target]
    (if (in-range? source* target* maxrange)
      (do
       (spawn-line-render {:start (start-point source* target*)
                           :end (:position target*)
                           :duration 0.05
                           :color [1 0 0 0.75]
                           :thick? true})
       (effects-do! ctx entity-effects))
      (spawn-audiovisual (end-point source* target* maxrange)
                         (db/build :audiovisuals/hit-ground)))))

(defn render-effect [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
  (when target
    (let [source* @source
          target* @target]
      (g/line (start-point source* target*)
              (end-point source* target* maxrange)
              (if (in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))
