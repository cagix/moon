(ns cdq.entity.stats
  (:require [cdq.stats :as stats]
            [cdq.val-max :as val-max]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn create [stats _ctx]
  (-> (if (:entity/mana stats)
        (update stats :entity/mana (fn [v] [v v]))
        stats)
      (update :entity/hp   (fn [v] [v v])))
  #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v]))))

(defn draw [_ entity {:keys [ctx/world-unit-scale]}]
  (let [ratio (val-max/ratio (stats/get-hitpoints (:creature/stats entity)))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar world-unit-scale
                  (:entity/body entity)
                  ratio))))
