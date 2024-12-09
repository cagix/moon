(ns forge.entity.hp
  (:require [anvil.entity :as entity]
            [anvil.graphics :as g]
            [anvil.val-max :as val-max]
            [clojure.gdx.graphics.color :as color]))

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

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (g/pixels->world-units 5)
          border (g/pixels->world-units borders-px)]
      (g/filled-rectangle x y width height color/black)
      (g/filled-rectangle (+ x border)
                          (+ y border)
                          (- (* width ratio)
                             (* 2 border))
                          (- height
                             (* 2 border))
                          (hpbar-color ratio)))))

(defn ->v [[_ v]]
  [v v])

(defn render-info [_ entity]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))
