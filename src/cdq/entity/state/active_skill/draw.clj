(ns cdq.entity.state.active-skill.draw
  (:require [cdq.effect :as effect]
            [cdq.graphics :as graphics]
            [gdl.timer :as timer]))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(defn- draw-skill-image
  [texture-region entity [x y] action-counter-ratio]
  (let [radius skill-image-radius-world-units
        y (+ (float y)
             (float (/ (:body/height (:entity/body entity)) 2))
             (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/texture-region texture-region [(- (float x) radius) y]]]))

(defn txs
  [{:keys [skill effect-ctx counter]}
   entity
   {:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image (graphics/texture-region graphics image)
                              entity
                              (:body/position (:entity/body entity))
                              (timer/ratio (:world/elapsed-time world) counter))
            (mapcat #(effect/draw % effect-ctx ctx)  ; update-effect-ctx here too ?
                    effects))))
