(ns cdq.entity.state.active-skill
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.stats :as stats]
            [cdq.timer :as timer]))

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(defn create [eid [skill effect-ctx] {:keys [world/elapsed-time]}]
  {:skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defn enter [{:keys [skill]} eid]
  [[:tx/sound (:skill/start-action-sound skill)]
   (when (:skill/cooldown skill)
     [:tx/set-cooldown eid skill])
   (when (and (:skill/cost skill)
              (not (zero? (:skill/cost skill))))
     [:tx/pay-mana-cost eid (:skill/cost skill)])])

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

(defn draw
  [{:keys [skill effect-ctx counter]}
   entity
   {:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image (graphics/texture-region graphics image)
                              entity
                              (entity/position entity)
                              (timer/ratio (:world/elapsed-time world) counter))
            (mapcat #(effect/draw % effect-ctx ctx)  ; update-effect-ctx here too ?
                    effects))))
