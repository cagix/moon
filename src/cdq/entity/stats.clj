(ns cdq.entity.stats
  (:require [cdq.entity :as entity]
            [cdq.val-max :as val-max]
            [cdq.utils :refer [defcomponent]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [ctx/world-unit-scale]}
                   {:keys [width half-width half-height]
                    :as entity}
                   ratio]
  (let [[x y] (entity/position entity)]
    (let [x (- x half-width)
          y (+ y half-height)
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defcomponent :creature/stats
  (entity/create [[_ stats] _ctx]
    (-> (if (:entity/mana stats)
          (update stats :entity/mana (fn [v] [v v]))
          stats)
        (update :entity/hp   (fn [v] [v v])))
    #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v]))))

  (entity/render-info! [_ entity c]
    (let [ratio (val-max/ratio (entity/hitpoints entity))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar c entity ratio)))))
