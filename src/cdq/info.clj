(ns cdq.info
  (:require [cdq.entity :as entity]
            [clojure.string :as str]
            [gdl.utils :refer [readable-number]]
            [gdl.info :as info :refer [info]]))

(defmethod info :property/pretty-name [[_ v] _c] v)
(defmethod info :maxrange             [[_ v] _c] v)

(defmethod info :creature/level [[_ v] _c]
  (str "Level: " v))

(defmethod info :projectile/piercing? [_ _c] ; TODO also when false ?!
  "Piercing")

(defmethod info :skill/action-time-modifier-key [[_ v] _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info :skill/action-time [[_ v] _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info :skill/cooldown [[_ v] _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info :skill/cost [[_ v] _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info ::stat [[k _] _c]
  (str (str/capitalize (name k)) ": " (entity/stat info/*info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
