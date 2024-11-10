(ns moon.effect.target-entity
  "🚧 Under construction ⚠️"
  (:require [gdl.graphics.shape-drawer :as sd]
            [gdl.math.vector :as v]
            [moon.body :as body]
            [moon.effect :as effect :refer [source target]]
            [moon.world.entities :as entities]))

(defn- in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

; TODO use at projectile & also adjust rotation
(defn- start-point [entity target*]
  (v/add (:position entity)
         (v/scale (body/direction entity target*)
                  (:radius entity))))

(defn- end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (body/direction entity target*)
                  maxrange)))

(defn applicable? [{:keys [entity-effects]}]
  (and target
       (effect/applicable? entity-effects)))

(defn useful? [{:keys [maxrange]}]
  (assert (bound? #'source))
  (assert (bound? #'target))
  (in-range? @source @target maxrange))

(defn handle [{:keys [maxrange entity-effects]}]
  (let [source* @source
        target* @target]
    (if (in-range? source* target* maxrange)
      (do
       (entities/line-render {:start (start-point source* target*)
                              :end (:position target*)
                              :duration 0.05
                              :color [1 0 0 0.75]
                              :thick? true})
       ; TODO => make new context with end-point ... and check on point entity
       ; friendly fire ?!
       ; player maybe just direction possible ?!
       ; TODO FIXME
       ; have to use tx/effect now ?!
       ; still same context ...
       ; filter applicable ?! - omg
       entity-effects)
      (do
       ; TODO
       ; * clicking on far away monster
       ; * hitting ground in front of you ( there is another monster )
       ; * -> it doesn't get hit ! hmmm
       ; * either use 'MISS' or get enemy entities at end-point
       (entities/audiovisual (end-point source* target* maxrange) :audiovisuals/hit-ground)
       nil))))

(defn render [{:keys [maxrange]}]
  (when target
    (let [source* @source
          target* @target]
      (sd/line (start-point source* target*)
               (end-point source* target* maxrange)
               (if (in-range? source* target* maxrange)
                 [1 0 0 0.5]
                 [1 1 0 0.5])))))
