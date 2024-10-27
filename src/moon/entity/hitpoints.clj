(ns moon.entity.hitpoints
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.entity.modifiers :refer [defstat]]
            [moon.graphics :as g]
            [moon.graphics.shape-drawer :as sd]
            [moon.val-max :as val-max]))

; TODO
; @ hp says here 'Minimum' hp instead of just 'HP'
; Sets to 0 but don't kills
; Could even set to a specific value ->
; op/set-to-ratio 0.5 ....
; sets the hp to 50%...
(defstat :stats/hp
  {:schema pos-int?
   :modifier-ops [:op/max-inc :op/max-mult]
   :effect-ops [:op/val-inc :op/val-mult :op/max-inc :op/max-mult]})

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
      (sd/filled-rectangle x y width height :black)
      (sd/filled-rectangle (+ x border)
                           (+ y border)
                           (- (* width ratio) (* 2 border))
                           (- height (* 2 border))
                           (hpbar-color ratio)))))

(defc :stats/hp
  (entity/->v [[_ v]]
    [v v])

  (entity/render-info [_ entity]
    (let [ratio (val-max/ratio (entity/stat entity :stats/hp))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar entity ratio)))))

