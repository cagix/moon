(ns anvil.entity.hp
  (:require [anvil.component :as component]
            [anvil.entity.modifiers :as mods]
            [anvil.info :as info]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]
            [gdl.val-max :as val-max]))

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
      (g/filled-rectangle x y width height g/black)
      (g/filled-rectangle (+ x border)
                          (+ y border)
                          (- (* width ratio)
                             (* 2 border))
                          (- height
                             (* 2 border))
                          (hpbar-color ratio)))))

(defn ->value
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (mods/apply-max-modifier entity :modifier/hp-max)))

(defmethods :entity/hp
  (component/info [_]
    (str "Hitpoints: " (->value info/*info-text-entity*)))

  (component/->v [[_ v]]
    [v v])

  (component/render-info [_ entity]
    (let [ratio (val-max/ratio (->value entity))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar entity ratio)))))
