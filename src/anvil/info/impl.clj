(ns anvil.info.impl
  (:require [anvil.entity :as entity]
            [gdl.info :as info]
            [clojure.utils :refer [readable-number]]))

(defmethod info/segment :property/pretty-name [[_ v] _c] v)
(defmethod info/segment :maxrange             [[_ v] _c] v)

(defmethod info/segment :creature/level [[_ v] _c]
  (str "Level: " v))

(defmethod info/segment :projectile/piercing? [_ _c] ; TODO also when false ?!
  "Piercing")

(defmethod info/segment :skill/action-time-modifier-key [[_ v] _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info/segment :skill/action-time [[_ v] _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info/segment :skill/cooldown [[_ v] _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info/segment :skill/cost [[_ v] _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info/segment ::stat [[k _] _c]
  (str (info/k->pretty-name k) ": " (entity/stat info/*info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
