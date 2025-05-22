(ns cdq.entity.hp
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]
            [cdq.val-max :as val-max]))

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

(defn- draw-hpbar [{:keys [ctx/world-unit-scale]
                    :as ctx}
                   {:keys [position width half-width half-height]} ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (* 5 world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defcomponent :entity/hp
  (entity/create [[_ v] _ctx]
    [v v])

  (entity/render-info! [_ entity ctx]
    (let [ratio (val-max/ratio (entity/hitpoints entity))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar ctx entity ratio)))))
