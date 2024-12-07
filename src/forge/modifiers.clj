(ns forge.modifiers
  (:require [forge.entity.modifiers :as mods]
            [forge.val-max :as val-max]
            [malli.core :as m]))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 1 mods/->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 0 mods/->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn damage-mods
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage-mods source damage)
           :damage/min-max
           apply-max-modifier
           target
           :modifier/damage-receive-max)))
