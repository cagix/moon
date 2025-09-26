(ns cdq.entity.state.active-skill
  (:require [cdq.effects.target-all :as target-all]
            [cdq.effects.target-entity :as target-entity]
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

(defn- render-target-entity
  [[_ {:keys [maxrange]}]
   {:keys [effect/source effect/target]}
   _ctx]
  (when target
    (let [source* @source
          target* @target]
      [[:draw/line
        (target-entity/start-point source* target*)
        (target-entity/end-point source* target* maxrange)
        (if (target-entity/in-range? source* target* maxrange)
          [1 0 0 0.5]
          [1 1 0 0.5])]])))

(defn- render-target-all
  [_
   {:keys [effect/source]}
   {:keys [ctx/world]}]
  (let [{:keys [world/active-entities]} world
        source* @source]
    (for [target* (map deref (target-all/affected-targets active-entities world source*))]
      [:draw/line
       (:body/position (:entity/body source*)) #_(start-point source* target*)
       (:body/position (:entity/body target*))
       [1 0 0 0.5]])))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(defn- render-effect [[k v] effect-ctx ctx]
  (case k
    :effects/target-entity (render-target-entity [k v] effect-ctx ctx)
    :effects/target-all    (render-target-all    [k v] effect-ctx ctx)
    nil
    ))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(render-effect % effect-ctx ctx) effect))

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
            (render-active-effect ctx
                                  effect-ctx ; update-effect-ctx?
                                  effects))))
