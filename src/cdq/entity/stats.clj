(ns cdq.entity.stats
  (:require [cdq.stats :as stats]
            [cdq.stats.ops :as ops]
            [cdq.val-max :as val-max]
            [gdl.graphics.color :as color]
            [malli.core :as m]))

(defn- get-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops/apply (modifier-k modifiers)
             base-value))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

; TODO can just pass ops instead of modifiers modifier-k
(defn- apply-max [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 1 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [(min v mx) mx]]
  (assert (m/validate val-max/schema result) result)
  result))

; TODO can just pass ops instead of modifiers modifier-k
(defn- apply-min [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 0 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [v (max v mx)]]
    (assert (m/validate val-max/schema result) result)
    result))

(defn- add*    [mods other-mods] (merge-with ops/add    mods other-mods))
(defn- remove* [mods other-mods] (merge-with ops/remove mods other-mods))

; 1. name ! :entity/ -> :stats/
; 2. tests/protocols -> what are data structure of modifiers => is stat-k
; witha modifier key????
; how does the whole thing look like
; including editor based omgfwtf

(defrecord Stats []
  stats/Stats
  (get-stat-value [stats stat-k]
    (when-let [base-value (stat-k stats)]
      (get-value base-value
                 (:entity/modifiers stats)
                 (keyword "modifier" (name stat-k)))))

  (add    [stats mods] (update stats :entity/modifiers add*    mods))
  (remove-mods [stats mods] (update stats :entity/modifiers remove* mods))

  (get-mana
    [{:keys [entity/mana
             entity/modifiers]}]
    (apply-max mana modifiers :modifier/mana-max))

  (mana-val [stats]
    (if (:entity/mana stats)
      ((stats/get-mana stats) 0) ; TODO fucking optional shit
      0))

  (not-enough-mana? [stats {:keys [skill/cost]}]
    (and cost (> cost (stats/mana-val stats))))

  (pay-mana-cost [stats cost]
    (let [mana-val (stats/mana-val stats)]
      (assert (<= cost mana-val))
      (assoc-in stats [:entity/mana 0] (- mana-val cost))))

  (get-hitpoints
    [{:keys [entity/hp
             entity/modifiers]}]
    (apply-max hp modifiers :modifier/hp-max))

  (damage [source damage]
    (update damage
            :damage/min-max
            #(-> %
                 (apply-min (:entity/modifiers source) :modifier/damage-deal-min)
                 (apply-max (:entity/modifiers source) :modifier/damage-deal-max))))

  (damage [source target damage]
    (update (stats/damage source damage)
            :damage/min-max
            apply-max
            (:entity/modifiers target)
            :modifier/damage-receive-max))

  ; FIXME I don't see it triggering with 10 armor save ... !
  (effective-armor-save [source-stats target-stats]
    (max (- (or (stats/get-stat-value source-stats :entity/armor-save)   0)
            (or (stats/get-stat-value target-stats :entity/armor-pierce) 0))
         0)))

(comment

 (effective-armor-save {} {:entity/modifiers {:modifiers/armor-save {:op/inc 10}}
                           :entity/armor-save 0})
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn create [stats _ctx]
  (map->Stats (-> (if (:entity/mana stats)
                    (update stats :entity/mana (fn [v] [v v]))
                    stats)
                  (update :entity/hp   (fn [v] [v v]))))

  #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v]))))

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
      [[:draw/filled-rectangle x y width height color/black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn draw [_ entity {:keys [ctx/graphics]}]
  (let [ratio (val-max/ratio (stats/get-hitpoints (:creature/stats entity)))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar (:graphics/world-unit-scale graphics)
                  (:entity/body entity)
                  ratio))))
