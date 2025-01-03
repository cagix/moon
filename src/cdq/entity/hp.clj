(ns cdq.entity.hp
  (:require [cdq.entity :as entity]
            [gdl.context :as c]
            [gdl.val-max :as val-max]))

(defn text [_ entity _c]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defn create [[_ v] _c]
  [v v])

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

(defn- draw-hpbar [c
                   {:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (c/pixels->world-units c 5)
          border (c/pixels->world-units c borders-px)]
      (c/filled-rectangle c x y width height :black)
      (c/filled-rectangle c
                          (+ x border)
                          (+ y border)
                          (- (* width ratio)
                             (* 2 border))
                          (- height
                             (* 2 border))
                          (hpbar-color ratio)))))

(defn render-info [_ entity c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar c entity ratio))))
